package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.constant.RegionConstant
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.ContentRating
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.ContentRatingsResponse
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TvHeroFactsTest {
    private fun episode(
        name: String,
        seasonNumber: Int = 1,
        episodeNumber: Int = 1,
    ) = Episode(id = name.hashCode().toLong(), name = name, seasonNumber = seasonNumber, episodeNumber = episodeNumber)

    @Test
    fun testAReturningSeriesIsOngoing() {
        assertTrue(TvDetail(status = TvStatusTestConstant.RETURNING).isOngoing)
    }

    /** A show TMDB has announced but not aired is still something a viewer can wait for. */
    @Test
    fun testPlannedAndInProductionCountAsOngoing() {
        assertTrue(TvDetail(status = TvStatusTestConstant.IN_PRODUCTION).isOngoing)
        assertTrue(TvDetail(status = TvStatusTestConstant.PLANNED).isOngoing)
    }

    @Test
    fun testEndedAndCancelledSeriesAreNotOngoing() {
        assertFalse(TvDetail(status = TvStatusTestConstant.ENDED).isOngoing)
        assertFalse(TvDetail(status = TvStatusTestConstant.CANCELED).isOngoing)
    }

    /** Status is nullable on the model, and a missing status must not claim the show is running. */
    @Test
    fun testAMissingStatusIsNotOngoing() {
        assertFalse(TvDetail().isOngoing)
    }

    /**
     * The badge shows the raw TMDB status, so the match has to be exact - a near-miss like
     * "returning series" would silently drop the badge rather than fail loudly.
     */
    @Test
    fun testStatusMatchingIsExact() {
        assertFalse(TvDetail(status = TvStatusTestConstant.RETURNING.lowercase()).isOngoing)
    }

    /** A running show is best entered at what is coming next. */
    @Test
    fun testPrefersTheNextEpisodeToAir() {
        val detail =
            TvDetail(
                nextEpisodeToAir = episode("What Comes Next", seasonNumber = 3, episodeNumber = 4),
                lastEpisodeToAir = episode("What Just Aired", seasonNumber = 3, episodeNumber = 3),
            )

        assertEquals("What Comes Next", detail.heroEpisode()?.name)
    }

    /** A finished show has no "next", so naming the last episode is the honest thing to do. */
    @Test
    fun testFallsBackToTheLastEpisodeWhenNothingIsScheduled() {
        val detail = TvDetail(lastEpisodeToAir = episode("The Finale", seasonNumber = 6, episodeNumber = 10))

        assertEquals("The Finale", detail.heroEpisode()?.name)
    }

    @Test
    fun testHasNoEpisodeWhenNothingHasAiredOrIsScheduled() {
        assertNull(TvDetail().heroEpisode())
    }

    @Test
    fun testContentRatingComesFromTheUsBucket() {
        val detail =
            TvDetail(
                contentRatings =
                    ContentRatingsResponse(
                        results =
                            listOf(
                                ContentRating(iso3166 = OTHER_REGION, rating = "15"),
                                ContentRating(iso3166 = RegionConstant.US, rating = "TV-MA"),
                            ),
                    ),
            )

        assertEquals("TV-MA", detail.usContentRating())
    }

    /** TMDB sends an empty string rather than omitting the entry for unrated shows. */
    @Test
    fun testTreatsAnEmptyRatingAsAbsent() {
        val detail =
            TvDetail(contentRatings = ContentRatingsResponse(results = listOf(ContentRating(iso3166 = RegionConstant.US, rating = ""))))

        assertNull(detail.usContentRating())
    }

    @Test
    fun testContentRatingIsNullWhenThereIsNoUsEntry() {
        val detail =
            TvDetail(contentRatings = ContentRatingsResponse(results = listOf(ContentRating(iso3166 = OTHER_REGION, rating = "15"))))

        assertNull(detail.usContentRating())
    }

    @Test
    fun testContentRatingIsNullWhenTheAppendIsMissing() {
        assertNull(TvDetail().usContentRating())
    }

    private companion object {
        /** Any region that is not the US bucket these accessors single out. */
        const val OTHER_REGION = "GB"
    }
}
