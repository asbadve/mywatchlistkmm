package com.ajinkyabadve.kmmmywatchlist.features.account.viewmodel

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TrackedTvShowSummary
import com.ajinkyabadve.kmmmywatchlist.features.account.model.UpcomingDateKind
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseSource
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseSourceKind
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Network
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

    @Test
    fun testBuildUpcomingEpisodeItems_labelsEpisodesWithTheNetworksCountry() {
        val show = TrackedTvShowSummary(id = SHOW_ID, title = SHOW_TITLE, posterPath = null)
        val season = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 1, airDate = FUTURE_AIR_DATE)))

        val item = buildUpcomingEpisodeItems(listOf(show to mapOf(1 to season)), TODAY, mapOf(SHOW_ID to NETWORK_REGION)).single()

        assertEquals(ReleaseSource(ReleaseSourceKind.EPISODE_AIR, NETWORK_REGION), item.releaseSource)
    }

    @Test
    fun testBuildUpcomingEpisodeItems_noKnownNetworkMeansNoRegion() {
        val show = TrackedTvShowSummary(id = SHOW_ID, title = SHOW_TITLE, posterPath = null)
        val season = TvSeasonDetail(seasonNumber = 1, episodes = listOf(Episode(episodeNumber = 1, airDate = FUTURE_AIR_DATE)))

        assertNull(buildUpcomingEpisodeItems(listOf(show to mapOf(1 to season)), TODAY).single().releaseSource)
    }

    @Test
    fun testAiringRegion_isTheFirstNetworkWithACountry() {
        val detail =
            TvDetail(networks = listOf(Network(name = NO_COUNTRY_NETWORK), Network(name = SHOW_NETWORK, originCountry = NETWORK_REGION)))

        assertEquals(NETWORK_REGION, detail.airingRegion())
        assertNull(TvDetail().airingRegion())
    }

    /** Streaming services often have no country on TMDB (Apple TV for Slow Horses). */
    @Test
    fun testAiringRegion_fallsBackToTheShowsOriginCountry() {
        val detail = TvDetail(networks = listOf(Network(name = STREAMING_NETWORK)), originCountry = listOf(ORIGIN_REGION))

        assertEquals(ORIGIN_REGION, detail.airingRegion())
    }

    private companion object {
        const val SHOW_ID = 9
        const val SHOW_TITLE = "The Simpsons"
        const val SHOW_NETWORK = "FOX"
        const val NO_COUNTRY_NETWORK = "Unknown"
        const val NETWORK_REGION = "US"
        const val STREAMING_NETWORK = "Apple TV"
        const val ORIGIN_REGION = "GB"
        const val FUTURE_AIR_DATE = "2099-01-01"
        val TODAY = LocalDate(2026, 1, 10)
    }
}
