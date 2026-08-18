package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TvTest {
    private val today = LocalDate(2026, 8, 17)

    @Test
    fun testFutureFirstAirDateIsUpcoming() {
        val tv = Tv(id = 1, firstAirDate = "2026-12-25")
        assertTrue(tv.isUpcoming(today))
    }

    @Test
    fun testPastFirstAirDateIsNotUpcoming() {
        val tv = Tv(id = 1, firstAirDate = "2020-01-01")
        assertFalse(tv.isUpcoming(today))
    }

    @Test
    fun testTodayIsNotUpcoming() {
        val tv = Tv(id = 1, firstAirDate = "2026-08-17")
        assertFalse(tv.isUpcoming(today))
    }

    @Test
    fun testBlankDateIsNotUpcoming() {
        val tv = Tv(id = 1, firstAirDate = "")
        assertFalse(tv.isUpcoming(today))
    }

    @Test
    fun testUnparsableDateIsNotUpcoming() {
        val tv = Tv(id = 1, firstAirDate = "not-a-date")
        assertFalse(tv.isUpcoming(today))
    }
}
