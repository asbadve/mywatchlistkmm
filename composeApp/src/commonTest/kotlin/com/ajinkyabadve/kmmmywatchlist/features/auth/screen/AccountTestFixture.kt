package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import com.ajinkyabadve.kmmmywatchlist.core.notification.FakeReminderScheduler
import com.ajinkyabadve.kmmmywatchlist.features.notifications.FakeNotificationDebugActions
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationJobSync
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderCoordinator
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeNotificationSettingsRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRestrictedModeRepository
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

/**
 * Fakes behind a real [AccountScreenModel], shared by the screen-model and UI tests.
 * [scheduleJobCalls]/[cancelJobCalls] count background-job starts and stops; [releaseReminderPolls]
 * counts reminder-date re-resolutions.
 */
class AccountTestFixture(
    restrictedModeEnabled: Boolean = false,
    episodeNotificationsEnabled: Boolean = false,
    releaseRemindersSupported: Boolean = true,
    backupSupported: Boolean = true,
    debugBuild: Boolean = true,
) {
    val regions =
        FakeRegionRepository(
            selectedRegion = AccountTestConstant.INITIAL_REGION,
            fallbackRegion = AccountTestConstant.INITIAL_FALLBACK_REGION,
        )
    val restrictedMode = FakeRestrictedModeRepository(restrictedModeEnabled = restrictedModeEnabled)
    val notificationSettings = FakeNotificationSettingsRepository(episodeNotificationsEnabled = episodeNotificationsEnabled)
    val reminders = FakeReleaseReminderRepository()
    val scheduler = FakeReminderScheduler()
    val debugActions = FakeNotificationDebugActions()
    var scheduleJobCalls = 0
        private set
    var cancelJobCalls = 0
        private set
    var releaseReminderPolls = 0
        private set

    val screenModel =
        AccountScreenModel(
            regionRepository = regions,
            restrictedModeRepository = restrictedMode,
            notificationSettingsRepository = notificationSettings,
            releaseReminderRepository = reminders,
            releaseReminderCoordinator =
                ReleaseReminderCoordinator(
                    repository = reminders,
                    scheduler = scheduler,
                    timeZone = { TimeZone.UTC },
                    now = { AccountTestConstant.NOW },
                ),
            notificationJobSync =
                NotificationJobSync(
                    notificationSettingsRepository = notificationSettings,
                    releaseReminderRepository = reminders,
                    scheduleJob = { scheduleJobCalls++ },
                    cancelJob = { cancelJobCalls++ },
                ),
            pollReleaseReminders = { releaseReminderPolls++ },
            notificationDebugActions = debugActions,
            releaseRemindersSupported = releaseRemindersSupported,
            backupSupported = backupSupported,
            debugBuild = debugBuild,
            is24Hour = false,
        )
}

object AccountTestConstant {
    const val INITIAL_REGION = "US"
    const val INITIAL_FALLBACK_REGION = "GB"
    const val NEW_REGION = "IN"
    const val NEW_FALLBACK_REGION = "DE"
    val NOW = LocalDateTime(2026, 10, 6, 8, 0)
}
