package com.ajinkyabadve.kmmmywatchlist.features.account.viewmodel

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.model.UpcomingDateKind
import com.ajinkyabadve.kmmmywatchlist.features.account.model.UpcomingMediaItem
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDateItem
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResult
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseSource
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseSourceKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResolveUpcomingMovieItemsTest {
    @Test
    fun testUsesTheViewersRegionalTheatricalDateAndRegion() {
        val detail = detail(REGION_IN to THEATRICAL_IN, REGION_US to THEATRICAL_US)

        val item = resolveUpcomingMovieItems(listOf(row() to detail), REGION_IN, TODAY).single()

        assertEquals(LocalDate.parse(THEATRICAL_IN_DATE), item.date)
        assertEquals(ReleaseSource(ReleaseSourceKind.THEATRICAL, REGION_IN), item.releaseSource)
    }

    @Test
    fun testFallsBackToTheUsDateWhenTheRegionHasNone() {
        val detail = detail(REGION_US to THEATRICAL_US)

        val item = resolveUpcomingMovieItems(listOf(row() to detail), REGION_IN, TODAY).single()

        assertEquals(LocalDate.parse(THEATRICAL_US_DATE), item.date)
        assertEquals(REGION_US, item.releaseSource?.regionCode)
    }

    @Test
    fun testFallsBackToThePrimaryDateWithNoRegionWhenNeitherHasOne() {
        val detail = detail(REGION_GB to THEATRICAL_US)

        val item = resolveUpcomingMovieItems(listOf(row() to detail), REGION_IN, TODAY).single()

        assertEquals(LocalDate.parse(PRIMARY_DATE), item.date)
        assertNull(item.releaseSource?.regionCode)
    }

    @Test
    fun testMovieWithNoCachedDetailKeepsItsRowAsIs() {
        val item = resolveUpcomingMovieItems(listOf(row() to null), REGION_IN, TODAY).single()

        assertEquals(row(), item)
    }

    @Test
    fun testMovieAlreadyOutInTheRegionIsDropped() {
        val detail = detail(REGION_IN to THEATRICAL_PAST)

        assertTrue(resolveUpcomingMovieItems(listOf(row() to detail), REGION_IN, TODAY).isEmpty())
    }

    /** Primary date already passed (out elsewhere first) but still to come here: keep it. */
    @Test
    fun testMovieOutElsewhereButStillToComeInTheRegionIsKept() {
        val detail = detail(REGION_IN to THEATRICAL_IN)

        val item = resolveUpcomingMovieItems(listOf(row(date = PAST_PRIMARY_DATE) to detail), REGION_IN, TODAY).single()

        assertEquals(LocalDate.parse(THEATRICAL_IN_DATE), item.date)
    }

    @Test
    fun testRowWithNoCachedDetailAndAPastPrimaryDateIsDropped() {
        assertTrue(resolveUpcomingMovieItems(listOf(row(date = PAST_PRIMARY_DATE) to null), REGION_IN, TODAY).isEmpty())
    }

    @Test
    fun testReminderFromAResolvedRowCarriesItsRegionalSource() {
        val item = resolveUpcomingMovieItems(listOf(row() to detail(REGION_IN to THEATRICAL_IN)), REGION_IN, TODAY).single()

        assertEquals(ReleaseSource(ReleaseSourceKind.THEATRICAL, REGION_IN).encode(), item.toReleaseReminder().releaseSource)
    }

    private fun row(date: String = PRIMARY_DATE) =
        UpcomingMediaItem(
            id = MOVIE_ID,
            mediaType = MediaTypeConstant.MOVIE,
            title = TITLE,
            posterPath = null,
            date = LocalDate.parse(date),
            dateKind = UpcomingDateKind.MOVIE_RELEASE,
        )

    private fun detail(vararg buckets: Pair<String, ReleaseDateItem>) =
        MovieDetail(
            releaseDate = PRIMARY_DATE,
            releaseDates =
                ReleaseDatesResponse(
                    results = buckets.map { (region, item) -> ReleaseDatesResult(iso3166 = region, releaseDates = listOf(item)) },
                ),
        )

    /** TMDB's primary date normally is the origin country's release - label it with that flag. */
    @Test
    fun testPrimaryDateIsLabelledWithTheOriginCountry() {
        val detail = detail(REGION_GB to THEATRICAL_US).copy(originCountry = listOf(REGION_GB))

        val item = resolveUpcomingMovieItems(listOf(row() to detail), REGION_IN, TODAY).single()

        assertEquals(LocalDate.parse(PRIMARY_DATE), item.date)
        assertEquals(ReleaseSource(ReleaseSourceKind.PRIMARY, REGION_GB), item.releaseSource)
    }

    private companion object {
        const val MOVIE_ID = 7
        const val TITLE = "The Mongoose"
        const val REGION_IN = "IN"
        const val REGION_US = "US"
        const val REGION_GB = "GB"
        const val THEATRICAL_TYPE = 3
        val TODAY = LocalDate(2026, 10, 17)
        const val PRIMARY_DATE = "2026-10-29"
        const val PAST_PRIMARY_DATE = "2026-10-01"
        const val THEATRICAL_IN_DATE = "2026-11-06"
        const val THEATRICAL_US_DATE = "2026-10-30"
        val THEATRICAL_IN = ReleaseDateItem(releaseDate = "${THEATRICAL_IN_DATE}T00:00:00.000Z", type = THEATRICAL_TYPE)
        val THEATRICAL_US = ReleaseDateItem(releaseDate = "${THEATRICAL_US_DATE}T00:00:00.000Z", type = THEATRICAL_TYPE)
        val THEATRICAL_PAST = ReleaseDateItem(releaseDate = "2026-10-10T00:00:00.000Z", type = THEATRICAL_TYPE)
    }
}
