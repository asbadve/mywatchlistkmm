package com.ajinkyabadve.kmmmywatchlist.features.search.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchResultItemTest {
    private val today = LocalDate(2026, 8, 17)

    @Test
    fun testFutureReleaseDateIsUpcoming() {
        val item = SearchResultItem(id = 1, releaseDate = "2026-12-25")
        assertTrue(item.isUpcoming(today))
    }

    @Test
    fun testFutureFirstAirDateIsUpcoming() {
        val item = SearchResultItem(id = 1, firstAirDate = "2027-01-01")
        assertTrue(item.isUpcoming(today))
    }

    @Test
    fun testPastReleaseDateIsNotUpcoming() {
        val item = SearchResultItem(id = 1, releaseDate = "2020-01-01")
        assertFalse(item.isUpcoming(today))
    }

    @Test
    fun testTodayIsNotUpcoming() {
        val item = SearchResultItem(id = 1, releaseDate = "2026-08-17")
        assertFalse(item.isUpcoming(today))
    }

    @Test
    fun testMissingDateIsNotUpcoming() {
        val item = SearchResultItem(id = 1)
        assertFalse(item.isUpcoming(today))
    }

    @Test
    fun testUnparsableDateIsNotUpcoming() {
        val item = SearchResultItem(id = 1, releaseDate = "not-a-date")
        assertFalse(item.isUpcoming(today))
    }
}
