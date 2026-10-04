package com.ajinkyabadve.kmmmywatchlist.core

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ClockTimeFormatTest {
    @Test
    fun testTwentyFourHour_padsHourAndMinute() {
        assertEquals("09:05", formatClockTime(LocalTime(9, 5), is24Hour = true, amLabel = AM, pmLabel = PM))
        assertEquals("21:30", formatClockTime(LocalTime(21, 30), is24Hour = true, amLabel = AM, pmLabel = PM))
    }

    @Test
    fun testTwelveHour_usesPeriodLabels() {
        assertEquals("9:00 AM", formatClockTime(LocalTime(9, 0), is24Hour = false, amLabel = AM, pmLabel = PM))
        assertEquals("6:30 PM", formatClockTime(LocalTime(18, 30), is24Hour = false, amLabel = AM, pmLabel = PM))
    }

    @Test
    fun testTwelveHour_midnightAndNoonAreTwelve() {
        assertEquals("12:00 AM", formatClockTime(LocalTime(0, 0), is24Hour = false, amLabel = AM, pmLabel = PM))
        assertEquals("12:00 PM", formatClockTime(LocalTime(12, 0), is24Hour = false, amLabel = AM, pmLabel = PM))
    }

    private companion object {
        const val AM = "AM"
        const val PM = "PM"
    }
}
