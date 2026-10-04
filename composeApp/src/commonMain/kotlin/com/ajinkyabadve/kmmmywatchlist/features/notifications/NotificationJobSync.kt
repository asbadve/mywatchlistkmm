package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.core.notification.NotificationScheduler
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.NotificationSettingsRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.NotificationSettingsRepositoryImpl

/**
 * Keeps the one shared 6-hour background job (`NotificationScheduler`) alive exactly while
 * something needs it: item 3's episode/person/collection alerts, or item 16's release reminders
 * (whose poller refreshes dates). Before reminders existed the job simply followed the episode
 * toggle; now turning episode alerts off mustn't stop reminder date refreshes, and vice versa.
 */
class NotificationJobSync(
    private val notificationSettingsRepository: NotificationSettingsRepository = NotificationSettingsRepositoryImpl(),
    private val releaseReminderRepository: ReleaseReminderRepository = ReleaseReminderRepositoryImpl(),
    private val scheduleJob: () -> Unit = { NotificationScheduler.schedule() },
    private val cancelJob: () -> Unit = { NotificationScheduler.cancel() },
) {
    suspend fun refresh() {
        if (notificationSettingsRepository.isEpisodeNotificationsEnabled() || hasActiveReminders(releaseReminderRepository)) {
            scheduleJob()
        } else {
            cancelJob()
        }
    }
}

/** True when release reminders are switched on and at least one is saved. */
internal suspend fun hasActiveReminders(repository: ReleaseReminderRepository): Boolean =
    repository.preference().enabled && repository.allReminders().isNotEmpty()

/**
 * What every platform's background job runs: item 3's pollers only while episode alerts are on,
 * and the release-reminder poller only while reminders are active. Each gate is checked here
 * rather than relying on the job's existence, since the job now serves both.
 */
suspend fun runBackgroundPolls(
    notificationSettingsRepository: NotificationSettingsRepository = NotificationSettingsRepositoryImpl(),
    releaseReminderRepository: ReleaseReminderRepository = ReleaseReminderRepositoryImpl(),
) {
    if (notificationSettingsRepository.isEpisodeNotificationsEnabled()) {
        TvEpisodeNotificationPoller().poll()
        PersonCreditNotificationPoller().poll()
        CollectionNotificationPoller().poll()
    }
    if (hasActiveReminders(releaseReminderRepository)) {
        ReleaseReminderPoller(repository = releaseReminderRepository).poll()
    }
}
