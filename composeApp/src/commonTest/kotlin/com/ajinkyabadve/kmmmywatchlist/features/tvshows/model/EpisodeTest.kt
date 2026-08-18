package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EpisodeTest {
    private val today = LocalDate(2026, 8, 17)

    @Test
    fun testPastAirDateIsReleased() {
        val episode = Episode(airDate = "2026-08-01")
        assertTrue(episode.isReleased(today))
    }

    @Test
    fun testTodayIsReleased() {
        val episode = Episode(airDate = "2026-08-17")
        assertTrue(episode.isReleased(today))
    }

    @Test
    fun testFutureAirDateIsNotReleased() {
        val episode = Episode(airDate = "2026-12-25")
        assertFalse(episode.isReleased(today))
    }

    @Test
    fun testMissingAirDateIsNotReleased() {
        val episode = Episode(airDate = null)
        assertFalse(episode.isReleased(today))
    }

    @Test
    fun testUnparsableAirDateIsNotReleased() {
        val episode = Episode(airDate = "not-a-date")
        assertFalse(episode.isReleased(today))
    }
}
