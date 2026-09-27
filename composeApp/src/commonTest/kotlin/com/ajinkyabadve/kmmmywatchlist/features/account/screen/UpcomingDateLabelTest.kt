package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class UpcomingDateLabelTest {
    @Test
    fun testResolveUpcomingDateLabel_sameDayIsToday() {
        assertEquals(UpcomingDateLabel.Today, resolveUpcomingDateLabel(TODAY, TODAY))
    }

    @Test
    fun testResolveUpcomingDateLabel_oneDayAheadIsTomorrow() {
        val date = LocalDate(2026, 1, 11)
        assertEquals(UpcomingDateLabel.Tomorrow, resolveUpcomingDateLabel(date, TODAY))
    }

    @Test
    fun testResolveUpcomingDateLabel_twoToSixDaysAheadIsInDays() {
        val date = LocalDate(2026, 1, 13)
        assertEquals(UpcomingDateLabel.InDays(3), resolveUpcomingDateLabel(date, TODAY))
    }

    @Test
    fun testResolveUpcomingDateLabel_sixDaysAheadIsStillInDays() {
        val date = LocalDate(2026, 1, 16)
        assertEquals(UpcomingDateLabel.InDays(6), resolveUpcomingDateLabel(date, TODAY))
    }

    @Test
    fun testResolveUpcomingDateLabel_sevenOrMoreDaysAheadIsPlainDate() {
        val date = LocalDate(2026, 1, 17)
        assertEquals(UpcomingDateLabel.PlainDate(date), resolveUpcomingDateLabel(date, TODAY))
    }

    private companion object {
        val TODAY = LocalDate(2026, 1, 10)
    }
}
