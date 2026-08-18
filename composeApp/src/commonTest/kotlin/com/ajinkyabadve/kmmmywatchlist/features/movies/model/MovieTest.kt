package com.ajinkyabadve.kmmmywatchlist.features.movies.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class MovieTest {
    private val today = LocalDate(2026, 8, 17)

    @Test
    fun testFutureReleaseDateIsUpcoming() {
        val movie = Movie(id = 1, releaseDate = "2026-12-25")
        assertTrue(movie.isUpcoming(today))
    }

    @Test
    fun testPastReleaseDateIsNotUpcoming() {
        val movie = Movie(id = 1, releaseDate = "2020-01-01")
        assertFalse(movie.isUpcoming(today))
    }

    @Test
    fun testTodayIsNotUpcoming() {
        val movie = Movie(id = 1, releaseDate = "2026-08-17")
        assertFalse(movie.isUpcoming(today))
    }

    @Test
    fun testBlankDateIsNotUpcoming() {
        val movie = Movie(id = 1, releaseDate = "")
        assertFalse(movie.isUpcoming(today))
    }

    @Test
    fun testUnparsableDateIsNotUpcoming() {
        val movie = Movie(id = 1, releaseDate = "not-a-date")
        assertFalse(movie.isUpcoming(today))
    }
}
