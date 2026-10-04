package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.REMINDER_TIME
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.movieReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderPreference
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeNotificationSettingsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationJobSyncTest {
    private var scheduleCount = 0
    private var cancelCount = 0

    private fun sync(
        episodeAlertsOn: Boolean,
        reminders: FakeReleaseReminderRepository,
    ) = NotificationJobSync(
        notificationSettingsRepository = FakeNotificationSettingsRepository(episodeNotificationsEnabled = episodeAlertsOn),
        releaseReminderRepository = reminders,
        scheduleJob = { scheduleCount++ },
        cancelJob = { cancelCount++ },
    )

    @Test
    fun testEpisodeAlertsOn_keepsTheJob() =
        runTest {
            sync(episodeAlertsOn = true, reminders = FakeReleaseReminderRepository()).refresh()

            assertEquals(1, scheduleCount)
            assertEquals(0, cancelCount)
        }

    @Test
    fun testOnlyRemindersActive_stillKeepsTheJob() =
        runTest {
            val reminders = FakeReleaseReminderRepository().apply { seed(movieReminder()) }

            sync(episodeAlertsOn = false, reminders = reminders).refresh()

            assertEquals(1, scheduleCount)
        }

    @Test
    fun testRemindersSwitchedOff_andNoEpisodeAlerts_cancelsTheJob() =
        runTest {
            val reminders =
                FakeReleaseReminderRepository(
                    ReminderPreference(enabled = false, time = REMINDER_TIME),
                ).apply { seed(movieReminder()) }

            sync(episodeAlertsOn = false, reminders = reminders).refresh()

            assertEquals(1, cancelCount)
            assertEquals(0, scheduleCount)
        }

    @Test
    fun testNothingNeedsIt_cancelsTheJob() =
        runTest {
            sync(episodeAlertsOn = false, reminders = FakeReleaseReminderRepository()).refresh()

            assertEquals(1, cancelCount)
        }
}
