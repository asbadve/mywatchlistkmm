package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.EPISODE_KEY
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.MOVIE_KEY
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.NOW
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.RELEASE_IN_FIVE_DAYS
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.RELEASE_TOMORROW
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.REMINDER_TIME
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReleaseReminderScheduleTest {
    @Test
    fun testFireTimes_dayBeforeAndReleaseDayBothAtTheChosenTime() {
        val fireTimes = reminderFireTimes(RELEASE_IN_FIVE_DAYS, REMINDER_TIME, NOW).toMap()

        assertEquals(RELEASE_IN_FIVE_DAYS.minus(DatePeriod(days = 1)).atTime(REMINDER_TIME), fireTimes[ReminderKind.DAY_BEFORE])
        assertEquals(RELEASE_IN_FIVE_DAYS.atTime(REMINDER_TIME), fireTimes[ReminderKind.RELEASE_DAY])
    }

    @Test
    fun testFireTimes_dayBeforeDroppedOnceItHasPassed() {
        // Releases tomorrow, but "now" is already past today's reminder time - only release day remains.
        val afterTodaysReminder = NOW.date.atTime(LATE_TIME)

        val kinds = reminderFireTimes(RELEASE_TOMORROW, REMINDER_TIME, afterTodaysReminder).map { it.first }

        assertEquals(listOf(ReminderKind.RELEASE_DAY), kinds)
    }

    @Test
    fun testFireTimes_bothDroppedAfterTheReleaseDayReminderPasses() {
        val afterRelease = RELEASE_TOMORROW.atTime(LATE_TIME)

        assertTrue(reminderFireTimes(RELEASE_TOMORROW, REMINDER_TIME, afterRelease).isEmpty())
    }

    @Test
    fun testFireTimes_beyondTheHorizonAreNotScheduled() {
        val farRelease = NOW.date.plus(DatePeriod(days = ReleaseReminderScheduleConstant.HORIZON_DAYS + HORIZON_OVERSHOOT_DAYS))

        assertTrue(reminderFireTimes(farRelease, REMINDER_TIME, NOW).isEmpty())
    }

    @Test
    fun testRequestIds_areStableAndDistinctPerKindAndEpisode() {
        assertEquals(MOVIE_KEY.requestId(ReminderKind.DAY_BEFORE), MOVIE_KEY.requestId(ReminderKind.DAY_BEFORE))
        assertNotEquals(MOVIE_KEY.requestId(ReminderKind.DAY_BEFORE), MOVIE_KEY.requestId(ReminderKind.RELEASE_DAY))
        assertNotEquals(
            EPISODE_KEY.requestId(ReminderKind.RELEASE_DAY),
            EPISODE_KEY.copy(episodeNumber = OTHER_EPISODE).requestId(ReminderKind.RELEASE_DAY),
        )
        assertEquals(ReminderKind.entries.size, MOVIE_KEY.allRequestIds().toSet().size)
    }

    @Test
    fun testRequestIds_allStartWithTheReleasePrefix() {
        assertTrue(EPISODE_KEY.allRequestIds().all { it.startsWith(ReleaseReminderScheduleConstant.ID_PREFIX) })
    }

    private companion object {
        val LATE_TIME = LocalTime(22, 0)
        const val HORIZON_OVERSHOOT_DAYS = 5
        const val OTHER_EPISODE = 9
    }
}
