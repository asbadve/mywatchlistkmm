package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.core.notification.FakeReminderScheduler
import com.ajinkyabadve.kmmmywatchlist.core.notification.MediaDetailNotificationTarget
import com.ajinkyabadve.kmmmywatchlist.core.notification.ReminderSchedulerConstant
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.EPISODE_KEY
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.LATER_REMINDER_TIME
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.MOVIE_KEY
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.NOW
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.RELEASE_IN_FIVE_DAYS
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.REMINDER_TIME
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.episodeReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.movieReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderPreference
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReleaseReminderCoordinatorTest {
    private val repository = FakeReleaseReminderRepository(ReminderPreference(enabled = true, time = REMINDER_TIME))
    private val scheduler = FakeReminderScheduler()
    private val coordinator =
        ReleaseReminderCoordinator(repository = repository, scheduler = scheduler, timeZone = { TimeZone.UTC }, now = { NOW })

    @Test
    fun testSetReminder_schedulesBothKindsAtTheChosenTime() =
        runTest {
            coordinator.setReminder(movieReminder(), enabled = true)

            assertEquals(RELEASE_IN_FIVE_DAYS.atTime(REMINDER_TIME), scheduler.scheduled[MOVIE_KEY.requestId(ReminderKind.RELEASE_DAY)])
            assertTrue(MOVIE_KEY.requestId(ReminderKind.DAY_BEFORE) in scheduler.scheduled)
            assertEquals(
                ReminderSchedulerConstant.DEFAULT_WINDOW_MINUTES,
                scheduler.windowMinutesById[MOVIE_KEY.requestId(ReminderKind.RELEASE_DAY)],
            )
        }

    @Test
    fun testSetReminder_tapTargetOpensTheTitle() =
        runTest {
            coordinator.setReminder(episodeReminder(), enabled = true)

            val target = scheduler.scheduledContent[EPISODE_KEY.requestId(ReminderKind.RELEASE_DAY)]?.target
            assertEquals(MediaDetailNotificationTarget(EPISODE_KEY.mediaId, EPISODE_KEY.mediaType), target)
        }

    @Test
    fun testRemovingAReminder_cancelsItsRequests() =
        runTest {
            coordinator.setReminder(movieReminder(), enabled = true)
            coordinator.setReminder(movieReminder(), enabled = false)

            assertTrue(scheduler.scheduled.isEmpty())
            assertTrue(scheduler.cancelledIds.containsAll(MOVIE_KEY.allRequestIds()))
        }

    @Test
    fun testDisabledPreference_cancelsWithoutScheduling() =
        runTest {
            repository.seed(movieReminder())
            repository.setPreference(ReminderPreference(enabled = false, time = REMINDER_TIME))

            coordinator.rescheduleAll()

            assertTrue(scheduler.scheduled.isEmpty())
        }

    @Test
    fun testChangingTheTime_movesEveryPendingReminder() =
        runTest {
            coordinator.setReminder(movieReminder(), enabled = true)

            coordinator.setPreference(ReminderPreference(enabled = true, time = LATER_REMINDER_TIME))

            assertEquals(
                RELEASE_IN_FIVE_DAYS.atTime(LATER_REMINDER_TIME),
                scheduler.scheduled[MOVIE_KEY.requestId(ReminderKind.RELEASE_DAY)],
            )
        }

    @Test
    fun testRescheduleAll_isIdempotent() =
        runTest {
            repository.seed(movieReminder(), episodeReminder())

            coordinator.rescheduleAll()
            val firstRun = scheduler.scheduled.toMap()
            coordinator.rescheduleAll()

            assertEquals(firstRun, scheduler.scheduled)
            assertEquals(EXPECTED_REQUESTS_FOR_TWO_REMINDERS, scheduler.scheduled.size)
        }

    @Test
    fun testClearAll_cancelsEverythingAndDeletesRows() =
        runTest {
            coordinator.setReminder(movieReminder(), enabled = true)

            coordinator.clearAll()

            assertTrue(scheduler.scheduled.isEmpty())
            assertTrue(repository.allReminders().isEmpty())
            assertEquals(1, repository.deleteAllCallCount)
        }

    @Test
    fun testTestReminder_usesTheTightestWindow() =
        runTest {
            coordinator.scheduleTestReminder()

            assertEquals(
                ReleaseReminderCoordinatorConstant.DEBUG_TEST_WINDOW_MINUTES,
                scheduler.windowMinutesById[ReleaseReminderCoordinatorConstant.DEBUG_TEST_REQUEST_ID],
            )
        }

    @Test
    fun testPendingForDebug_reportsWhatTheDatabaseExpects() =
        runTest {
            coordinator.setReminder(movieReminder(), enabled = true)

            val pending = coordinator.pendingForDebug()

            assertEquals(MOVIE_KEY.allRequestIds().toSet(), pending.map { it.id }.toSet())
            assertTrue(pending.all { it.isRegistered })
        }

    private companion object {
        const val EXPECTED_REQUESTS_FOR_TWO_REMINDERS = 4
    }
}
