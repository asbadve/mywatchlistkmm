package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedTvShowSummary
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.UpcomingDateKind
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildUpcomingEpisodeItemsTest {
    @Test
    fun testBuildUpcomingEpisodeItems_includesEveryUnreleasedEpisodeOfASeason() {
        val show = TrackedTvShowSummary(id = 1, title = "Lanterns", posterPath = null)
        val season =
            TvSeasonDetail(
                seasonNumber = 2,
                episodes =
                    listOf(
                        Episode(episodeNumber = 4, airDate = "2000-01-01"), // already released
                        Episode(episodeNumber = 5, airDate = "2099-01-01"),
                        Episode(episodeNumber = 6, airDate = "2099-01-08"),
                    ),
            )

        val items = buildUpcomingEpisodeItems(listOf(show to mapOf(2 to season)), TODAY)

        assertEquals(2, items.size)
        assertEquals(listOf(5, 6), items.map { it.episodeNumber })
        assertTrue(items.all { it.id == 1 && it.mediaType == MediaTypeConstant.TV && it.dateKind == UpcomingDateKind.NEXT_EPISODE })
        assertTrue(items.all { it.seasonNumber == 2 })
    }

    @Test
    fun testBuildUpcomingEpisodeItems_excludesEpisodeWithNoAirDate() {
        val show = TrackedTvShowSummary(id = 1, title = "Lanterns", posterPath = null)
        val season = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 1, airDate = null)))

        assertTrue(buildUpcomingEpisodeItems(listOf(show to mapOf(1 to season)), TODAY).isEmpty())
    }

    @Test
    fun testBuildUpcomingEpisodeItems_excludesEpisodeWithUnparsableAirDate() {
        val show = TrackedTvShowSummary(id = 1, title = "Lanterns", posterPath = null)
        val season = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 1, airDate = "not-a-date")))

        assertTrue(buildUpcomingEpisodeItems(listOf(show to mapOf(1 to season)), TODAY).isEmpty())
    }

    @Test
    fun testBuildUpcomingEpisodeItems_combinesEpisodesAcrossMultipleShows() {
        val showOne = TrackedTvShowSummary(id = 1, title = "Show One", posterPath = null)
        val showTwo = TrackedTvShowSummary(id = 2, title = "Show Two", posterPath = null)
        val seasonOne = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 1, airDate = "2099-01-01")))
        val seasonTwo = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 1, airDate = "2099-02-01")))

        val items =
            buildUpcomingEpisodeItems(
                listOf(showOne to mapOf(1 to seasonOne), showTwo to mapOf(1 to seasonTwo)),
                TODAY,
            )

        assertEquals(setOf("Show One", "Show Two"), items.map { it.title }.toSet())
    }

    private companion object {
        val TODAY = LocalDate(2026, 1, 10)
    }
}
