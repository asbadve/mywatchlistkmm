package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDateItem
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResult
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.REGION_GB
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.REGION_IN
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.REGION_US
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderTestConstant.TODAY
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleaseDateResolutionTest {
    @Test
    fun testMovie_regionalTheatricalBeatsDigitalAndPrimary() {
        val detail =
            movie(
                primary = PRIMARY_DATE,
                REGION_IN to listOf(item(ReleaseType.DIGITAL, DIGITAL_DATE), item(ReleaseType.THEATRICAL, THEATRICAL_DATE)),
            )

        val resolved = detail.resolveReleaseDate(REGION_IN, REGION_US)

        assertEquals(LocalDate.parse(THEATRICAL_DATE_PLAIN), resolved?.date)
        assertEquals(ReleaseSource(ReleaseSourceKind.THEATRICAL, REGION_IN), resolved?.source)
    }

    @Test
    fun testMovie_digitalUsedWhenRegionHasNoTheatrical() {
        val detail = movie(primary = PRIMARY_DATE, REGION_IN to listOf(item(ReleaseType.DIGITAL, DIGITAL_DATE)))

        assertEquals(ReleaseSourceKind.DIGITAL, detail.resolveReleaseDate(REGION_IN, REGION_US)?.source?.kind)
    }

    @Test
    fun testMovie_premiereIsNeverChosen() {
        val detail = movie(primary = PRIMARY_DATE, REGION_IN to listOf(item(ReleaseType.PREMIERE, PREMIERE_DATE)))

        val resolved = detail.resolveReleaseDate(REGION_IN, REGION_IN)

        assertEquals(ReleaseSourceKind.PRIMARY, resolved?.source?.kind)
        assertEquals(LocalDate.parse(PRIMARY_DATE), resolved?.date)
    }

    @Test
    fun testMovie_fallbackRegionUsedBeforePrimary() {
        val detail = movie(primary = PRIMARY_DATE, REGION_US to listOf(item(ReleaseType.THEATRICAL, THEATRICAL_DATE)))

        assertEquals(REGION_US, detail.resolveReleaseDate(REGION_IN, REGION_US)?.source?.regionCode)
    }

    @Test
    fun testMovie_unrelatedRegionIsIgnored() {
        // A third country's date means nothing to this viewer - the primary date is used instead.
        val detail = movie(primary = PRIMARY_DATE, REGION_GB to listOf(item(ReleaseType.THEATRICAL, THEATRICAL_DATE)))

        assertEquals(ReleaseSourceKind.PRIMARY, detail.resolveReleaseDate(REGION_IN, REGION_US)?.source?.kind)
    }

    @Test
    fun testMovie_earliestOfTheTypeWinsOverReReleases() {
        val detail =
            movie(
                primary = PRIMARY_DATE,
                REGION_IN to listOf(item(ReleaseType.THEATRICAL, RE_RELEASE_DATE), item(ReleaseType.THEATRICAL, THEATRICAL_DATE)),
            )

        assertEquals(LocalDate.parse(THEATRICAL_DATE_PLAIN), detail.resolveReleaseDate(REGION_IN, REGION_US)?.date)
    }

    @Test
    fun testMovie_noUsableDateResolvesToNull() {
        assertNull(MovieDetail(releaseDate = "").resolveReleaseDate(REGION_IN, REGION_US))
    }

    @Test
    fun testTv_resolvesToFirstAirDate() {
        val resolved = TvDetail(firstAirDate = PRIMARY_DATE).resolveReleaseDate()

        assertEquals(ReleaseSource(ReleaseSourceKind.FIRST_AIR), resolved?.source)
        assertEquals(LocalDate.parse(PRIMARY_DATE), resolved?.date)
    }

    @Test
    fun testIsUpcoming_onlyForAParseableFutureDate() {
        assertTrue(MovieDetail(releaseDate = FUTURE_DATE).isUpcoming(TODAY))
        assertFalse(MovieDetail(releaseDate = PAST_DATE).isUpcoming(TODAY))
        assertFalse(MovieDetail(releaseDate = YEAR_ONLY).isUpcoming(TODAY))
        assertTrue(TvDetail(firstAirDate = FUTURE_DATE).isUpcoming(TODAY))
        assertFalse(TvDetail(firstAirDate = "").isUpcoming(TODAY))
    }

    @Test
    fun testRegionalReleases_listsTheRegionOldestFirstAndSkipsUnknownTypes() {
        val detail =
            movie(
                primary = PRIMARY_DATE,
                REGION_IN to listOf(item(ReleaseType.DIGITAL, DIGITAL_DATE), item(ReleaseType.THEATRICAL, THEATRICAL_DATE)),
                REGION_US to listOf(item(ReleaseType.PREMIERE, PREMIERE_DATE)),
            ).let { it.copy(releaseDates = it.releaseDates?.copy(results = it.releaseDates.results + unknownTypeBucket())) }

        val releases = detail.regionalReleases(REGION_IN)

        assertEquals(listOf(ReleaseType.THEATRICAL, ReleaseType.DIGITAL), releases.map { it.type })
        assertTrue(detail.regionalReleases(REGION_GB).isEmpty())
    }

    @Test
    fun testReleaseSource_roundTripsAndToleratesUnknownValues() {
        val source = ReleaseSource(ReleaseSourceKind.THEATRICAL, REGION_IN)

        assertEquals(source, ReleaseSource.decode(source.encode()))
        assertEquals(ReleaseSource(ReleaseSourceKind.PRIMARY), ReleaseSource.decode(null))
        assertEquals(ReleaseSourceKind.PRIMARY, ReleaseSource.decode(UNKNOWN_SOURCE).kind)
    }

    private fun unknownTypeBucket() =
        ReleaseDatesResult(iso3166 = REGION_GB, releaseDates = listOf(ReleaseDateItem(releaseDate = THEATRICAL_DATE, type = UNKNOWN_TYPE)))

    private fun item(
        type: ReleaseType,
        isoDateTime: String,
    ) = ReleaseDateItem(releaseDate = isoDateTime, type = type.tmdbValue)

    private fun movie(
        primary: String,
        vararg buckets: Pair<String, List<ReleaseDateItem>>,
    ) = MovieDetail(
        releaseDate = primary,
        releaseDates =
            ReleaseDatesResponse(
                results =
                    buckets.map { (region, items) ->
                        ReleaseDatesResult(iso3166 = region, releaseDates = items)
                    },
            ),
    )

    private companion object {
        const val PRIMARY_DATE = "2026-11-01"
        const val THEATRICAL_DATE_PLAIN = "2026-11-06"
        const val THEATRICAL_DATE = "2026-11-06T00:00:00.000Z"
        const val DIGITAL_DATE = "2026-12-20T00:00:00.000Z"
        const val PREMIERE_DATE = "2026-10-10T00:00:00.000Z"
        const val RE_RELEASE_DATE = "2027-05-01T00:00:00.000Z"
        const val FUTURE_DATE = "2026-12-25"
        const val PAST_DATE = "2026-01-01"
        const val YEAR_ONLY = "2027"
        const val UNKNOWN_SOURCE = "festival:XX"
        const val UNKNOWN_TYPE = 99
    }
}
