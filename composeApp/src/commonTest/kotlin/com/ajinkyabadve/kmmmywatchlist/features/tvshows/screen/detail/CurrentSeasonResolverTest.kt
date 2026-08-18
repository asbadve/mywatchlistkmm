package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrentSeasonResolverTest {
    private val today = LocalDate(2026, 8, 17)

    @Test
    fun testPicksSeasonAndEpisodeOfTheLatestReleasedEpisodeAcrossSeasons() {
        val seasonDetails =
            mapOf(
                1 to
                    TvSeasonDetail(
                        seasonNumber = 1,
                        episodes = listOf(Episode(seasonNumber = 1, episodeNumber = 1, airDate = "2025-01-01")),
                    ),
                2 to
                    TvSeasonDetail(
                        seasonNumber = 2,
                        episodes =
                            listOf(
                                Episode(seasonNumber = 2, episodeNumber = 1, airDate = "2026-01-01"),
                                // Not yet released - must not be picked even though it's the highest episode number.
                                Episode(seasonNumber = 2, episodeNumber = 2, airDate = "2099-01-01"),
                            ),
                    ),
            )

        val (seasonNumber, episodeNumber) = resolveCurrentSeasonAndEpisode(seasonDetails, today)

        assertEquals(2, seasonNumber)
        assertEquals(1, episodeNumber)
    }

    @Test
    fun testIgnoresAnUnreleasedFutureSeasonEntirely() {
        val seasonDetails =
            mapOf(
                1 to
                    TvSeasonDetail(
                        seasonNumber = 1,
                        episodes = listOf(Episode(seasonNumber = 1, episodeNumber = 5, airDate = "2025-06-01")),
                    ),
                2 to
                    TvSeasonDetail(
                        seasonNumber = 2,
                        episodes = listOf(Episode(seasonNumber = 2, episodeNumber = 1, airDate = "2099-01-01")),
                    ),
            )

        val (seasonNumber, episodeNumber) = resolveCurrentSeasonAndEpisode(seasonDetails, today)

        assertEquals(1, seasonNumber)
        assertEquals(5, episodeNumber)
    }

    @Test
    fun testTodayCountsAsReleased() {
        val seasonDetails =
            mapOf(
                1 to
                    TvSeasonDetail(
                        seasonNumber = 1,
                        episodes = listOf(Episode(seasonNumber = 1, episodeNumber = 1, airDate = "2026-08-17")),
                    ),
            )

        val (seasonNumber, episodeNumber) = resolveCurrentSeasonAndEpisode(seasonDetails, today)

        assertEquals(1, seasonNumber)
        assertEquals(1, episodeNumber)
    }

    @Test
    fun testFallsBackToEarliestSeasonWithNullEpisodeWhenNothingHasReleasedYet() {
        val seasonDetails =
            mapOf(
                0 to
                    TvSeasonDetail(
                        seasonNumber = 0,
                        episodes = listOf(Episode(seasonNumber = 0, episodeNumber = 1, airDate = "2099-01-01")),
                    ),
                1 to
                    TvSeasonDetail(
                        seasonNumber = 1,
                        episodes = listOf(Episode(seasonNumber = 1, episodeNumber = 1, airDate = "2099-06-01")),
                    ),
                2 to
                    TvSeasonDetail(
                        seasonNumber = 2,
                        episodes = listOf(Episode(seasonNumber = 2, episodeNumber = 1, airDate = "2099-12-01")),
                    ),
            )

        val (seasonNumber, episodeNumber) = resolveCurrentSeasonAndEpisode(seasonDetails, today)

        // Season 0 ("Specials") is skipped in favor of season 1 when a positively-numbered season exists.
        assertEquals(1, seasonNumber)
        assertEquals(null, episodeNumber)
    }

    @Test
    fun testTiedAirDatesBreakTowardTheHigherEpisodeNumber() {
        val seasonDetails =
            mapOf(
                1 to
                    TvSeasonDetail(
                        seasonNumber = 1,
                        episodes =
                            listOf(
                                Episode(seasonNumber = 1, episodeNumber = 1, airDate = "2026-01-01"),
                                Episode(seasonNumber = 1, episodeNumber = 4, airDate = "2026-01-01"),
                                Episode(seasonNumber = 1, episodeNumber = 2, airDate = "2026-01-01"),
                                Episode(seasonNumber = 1, episodeNumber = 3, airDate = "2026-01-01"),
                            ),
                    ),
            )

        val (seasonNumber, episodeNumber) = resolveCurrentSeasonAndEpisode(seasonDetails, today)

        assertEquals(1, seasonNumber)
        assertEquals(4, episodeNumber)
    }

    @Test
    fun testTiedAirDatesAcrossSeasonsBreakTowardTheHigherSeasonNumber() {
        val seasonDetails =
            mapOf(
                1 to
                    TvSeasonDetail(
                        seasonNumber = 1,
                        episodes = listOf(Episode(seasonNumber = 1, episodeNumber = 8, airDate = "2026-01-01")),
                    ),
                2 to
                    TvSeasonDetail(
                        seasonNumber = 2,
                        episodes = listOf(Episode(seasonNumber = 2, episodeNumber = 1, airDate = "2026-01-01")),
                    ),
            )

        val (seasonNumber, episodeNumber) = resolveCurrentSeasonAndEpisode(seasonDetails, today)

        assertEquals(2, seasonNumber)
        assertEquals(1, episodeNumber)
    }

    @Test
    fun testEmptySeasonDetailsResolvesToNulls() {
        val (seasonNumber, episodeNumber) = resolveCurrentSeasonAndEpisode(emptyMap(), today)

        assertEquals(null, seasonNumber)
        assertEquals(null, episodeNumber)
    }
}
