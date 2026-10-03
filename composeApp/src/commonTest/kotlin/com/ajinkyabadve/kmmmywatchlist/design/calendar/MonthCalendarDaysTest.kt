package com.ajinkyabadve.kmmmywatchlist.design.calendar

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonthCalendarDaysTest {
    @Test
    fun testBuildCalendarDays_octoberTwentyTwentySixStartsOnThursday() {
        // October 1st 2026 is a Thursday - 3 leading blanks for a Monday-first grid.
        val days = buildCalendarDays(year = 2026, monthNumber = 10, markedDays = emptySet(), today = LocalDate(2026, 10, 3))
        val leadingBlanks = days.takeWhile { it.dayOfMonth == null }
        assertEquals(3, leadingBlanks.size)
        assertEquals(31, days.count { it.dayOfMonth != null })
    }

    @Test
    fun testBuildCalendarDays_marksTodayAndNoOtherDay() {
        val days = buildCalendarDays(year = 2026, monthNumber = 10, markedDays = emptySet(), today = LocalDate(2026, 10, 3))
        assertTrue(days.single { it.dayOfMonth == 3 }.isToday)
        assertFalse(days.single { it.dayOfMonth == 4 }.isToday)
    }

    @Test
    fun testBuildCalendarDays_marksEachMarkedDayAndNoOther() {
        val days =
            buildCalendarDays(year = 2026, monthNumber = 10, markedDays = setOf(4, 16), today = LocalDate(2026, 10, 3))
        assertTrue(days.single { it.dayOfMonth == 4 }.isMarked)
        assertTrue(days.single { it.dayOfMonth == 16 }.isMarked)
        assertFalse(days.single { it.dayOfMonth == 5 }.isMarked)
    }

    @Test
    fun testBuildCalendarDays_februaryLeapYearHasTwentyNineDays() {
        val days = buildCalendarDays(year = 2028, monthNumber = 2, markedDays = emptySet(), today = LocalDate(2028, 2, 1))
        assertEquals(29, days.count { it.dayOfMonth != null })
    }

    @Test
    fun testBuildCalendarDays_februaryNonLeapYearHasTwentyEightDays() {
        val days = buildCalendarDays(year = 2026, monthNumber = 2, markedDays = emptySet(), today = LocalDate(2026, 2, 1))
        assertEquals(28, days.count { it.dayOfMonth != null })
    }
}
