package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.constant.RegionConstant
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.HeroTestConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.RegionWatchProviders
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDateItem
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvider
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvidersResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MovieHeroFactsTest {
    private fun provider(
        name: String,
        displayPriority: Int = 0,
    ) = WatchProvider(providerId = name.hashCode(), providerName = name, displayPriority = displayPriority)

    private fun region(
        link: String = HeroTestConstant.MOVIE_WATCH_PAGE_LINK,
        flatrate: List<WatchProvider> = emptyList(),
        free: List<WatchProvider> = emptyList(),
        ads: List<WatchProvider> = emptyList(),
        rent: List<WatchProvider> = emptyList(),
        buy: List<WatchProvider> = emptyList(),
    ) = RegionWatchProviders(link = link, flatrate = flatrate, free = free, ads = ads, rent = rent, buy = buy)

    /**
     * The ranking is the whole point of the hero button: a subscription the viewer may already hold
     * beats being asked to pay, so flatrate wins even when a rental exists for the same title.
     */
    @Test
    fun testPrefersSubscriptionOverRental() {
        val providers =
            WatchProvidersResponse(
                results =
                    mapOf(
                        VIEWER_REGION to
                            region(
                                flatrate = listOf(provider(HeroTestConstant.PROVIDER_NETFLIX)),
                                rent = listOf(provider("Apple TV")),
                            ),
                    ),
            )

        assertEquals(HeroTestConstant.PROVIDER_NETFLIX, providers.heroWatchOption(VIEWER_REGION, RegionConstant.US)?.provider?.providerName)
    }

    /** Free-with-ads still costs nothing at the point of tapping, so it outranks rent and buy. */
    @Test
    fun testPrefersFreeAndAdsOverPaidOptions() {
        val providers =
            WatchProvidersResponse(
                results = mapOf(VIEWER_REGION to region(ads = listOf(provider("ITVX")), buy = listOf(provider("Apple TV")))),
            )

        assertEquals("ITVX", providers.heroWatchOption(VIEWER_REGION, RegionConstant.US)?.provider?.providerName)
    }

    /** Within a tier TMDB's own display priority decides, so the button matches what TMDB shows. */
    @Test
    fun testOrdersWithinATierByDisplayPriority() {
        val providers =
            WatchProvidersResponse(
                results =
                    mapOf(
                        VIEWER_REGION to
                            region(
                                flatrate =
                                    listOf(
                                        provider("Second Choice", displayPriority = 9),
                                        provider("First Choice", displayPriority = 1),
                                    ),
                            ),
                    ),
            )
        val option = providers.heroWatchOption(VIEWER_REGION, RegionConstant.US)

        assertEquals("First Choice", option?.provider?.providerName)
        assertEquals(listOf("First Choice", "Second Choice"), option?.allProviders?.map { it.providerName })
    }

    @Test
    fun testUsesTheViewersOwnRegionWhenItHasData() {
        val providers =
            WatchProvidersResponse(
                results =
                    mapOf(
                        RegionConstant.US to region(flatrate = listOf(provider("Hulu"))),
                        VIEWER_REGION to region(flatrate = listOf(provider("Sky"))),
                    ),
            )

        assertEquals("Sky", providers.heroWatchOption(VIEWER_REGION, RegionConstant.US)?.provider?.providerName)
    }

    /** US is the fallback because it is the region TMDB most reliably has data for. */
    @Test
    fun testFallsBackToUsWhenTheViewersRegionHasNoData() {
        val providers = WatchProvidersResponse(results = mapOf(RegionConstant.US to region(flatrate = listOf(provider("Hulu")))))

        assertEquals("Hulu", providers.heroWatchOption(VIEWER_REGION, RegionConstant.US)?.provider?.providerName)
    }

    /** The fallback region is a caller-supplied setting, not always US - honor whatever is passed. */
    @Test
    fun testFallsBackToTheCallersChosenFallbackRegionWhenItIsNotUs() {
        val providers = WatchProvidersResponse(results = mapOf("DE" to region(flatrate = listOf(provider("WOW")))))

        assertEquals("WOW", providers.heroWatchOption(VIEWER_REGION, "DE")?.provider?.providerName)
    }

    /** Naming somewhere it streams beats saying nothing, even if it is not the viewer's country. */
    @Test
    fun testFallsBackToAnyRegionWithDataWhenEvenUsIsMissing() {
        val providers = WatchProvidersResponse(results = mapOf("DE" to region(flatrate = listOf(provider("WOW")))))

        assertEquals("WOW", providers.heroWatchOption(VIEWER_REGION, RegionConstant.US)?.provider?.providerName)
    }

    /** The link is TMDB's watch page, which carries the JustWatch attribution TMDB requires. */
    @Test
    fun testCarriesTheRegionsWatchPageLink() {
        val providers =
            WatchProvidersResponse(
                results = mapOf(VIEWER_REGION to region(flatrate = listOf(provider(HeroTestConstant.PROVIDER_NETFLIX)))),
            )

        assertEquals(HeroTestConstant.MOVIE_WATCH_PAGE_LINK, providers.heroWatchOption(VIEWER_REGION, RegionConstant.US)?.link)
    }

    /** A region entry with every tier empty must not produce a button pointing at nothing. */
    @Test
    fun testReturnsNullWhenTheRegionHasNoProvidersInAnyTier() {
        val providers = WatchProvidersResponse(results = mapOf(VIEWER_REGION to region()))

        assertNull(providers.heroWatchOption(VIEWER_REGION, RegionConstant.US))
    }

    @Test
    fun testReturnsNullWhenThereAreNoRegionsAtAll() {
        assertNull(WatchProvidersResponse().heroWatchOption(VIEWER_REGION, RegionConstant.US))
    }

    /** The watch/providers append is optional, so the whole object can be absent. */
    @Test
    fun testReturnsNullWhenTheProvidersAppendIsMissing() {
        val absent: WatchProvidersResponse? = null

        assertNull(absent.heroWatchOption(VIEWER_REGION, RegionConstant.US))
    }

    /** The movie-side entry point must resolve the same option as the shared one it delegates to. */
    @Test
    fun testMovieDetailResolvesThroughToItsOwnProviders() {
        val detail =
            MovieDetail(
                watchProviders =
                    WatchProvidersResponse(
                        results = mapOf(VIEWER_REGION to region(flatrate = listOf(provider(HeroTestConstant.PROVIDER_NETFLIX)))),
                    ),
            )

        assertEquals(HeroTestConstant.PROVIDER_NETFLIX, detail.heroWatchOption(VIEWER_REGION, RegionConstant.US)?.provider?.providerName)
    }

    @Test
    fun testCertificationComesFromTheUsReleaseDates() {
        val detail =
            MovieDetail(
                releaseDates =
                    ReleaseDatesResponse(
                        results =
                            listOf(
                                ReleaseDatesResult(
                                    iso3166 = VIEWER_REGION,
                                    releaseDates = listOf(ReleaseDateItem(certification = "15")),
                                ),
                                ReleaseDatesResult(
                                    iso3166 = RegionConstant.US,
                                    releaseDates = listOf(ReleaseDateItem(certification = "R")),
                                ),
                            ),
                    ),
            )

        assertEquals("R", detail.usCertification())
    }

    /**
     * TMDB lists one entry per release type (theatrical, digital, physical) and only some carry a
     * certification, so the first non-empty one is the rating - not the first entry.
     */
    @Test
    fun testCertificationSkipsReleaseTypesThatCarryNoRating() {
        val detail =
            MovieDetail(
                releaseDates =
                    ReleaseDatesResponse(
                        results =
                            listOf(
                                ReleaseDatesResult(
                                    iso3166 = RegionConstant.US,
                                    releaseDates = listOf(ReleaseDateItem(certification = ""), ReleaseDateItem(certification = "PG-13")),
                                ),
                            ),
                    ),
            )

        assertEquals("PG-13", detail.usCertification())
    }

    @Test
    fun testCertificationIsNullWhenThereIsNoUsRelease() {
        val detail =
            MovieDetail(
                releaseDates =
                    ReleaseDatesResponse(
                        results =
                            listOf(
                                ReleaseDatesResult(
                                    iso3166 = VIEWER_REGION,
                                    releaseDates = listOf(ReleaseDateItem(certification = "15")),
                                ),
                            ),
                    ),
            )

        assertNull(detail.usCertification())
    }

    @Test
    fun testCertificationIsNullWhenTheReleaseDatesAppendIsMissing() {
        assertNull(MovieDetail().usCertification())
    }

    @Test
    fun testSplitsRuntimeIntoHoursAndMinutes() {
        assertEquals(2 to 22, MovieDetail(runtime = 142).runtimeHoursAndMinutes())
    }

    /** Shorts run under an hour; the hero drops the "0h" rather than printing it. */
    @Test
    fun testShortRuntimesHaveNoWholeHours() {
        assertEquals(0 to 45, MovieDetail(runtime = 45).runtimeHoursAndMinutes())
    }

    @Test
    fun testExactHoursHaveNoLeftoverMinutes() {
        assertEquals(2 to 0, MovieDetail(runtime = 120).runtimeHoursAndMinutes())
    }

    /** TMDB sends 0 rather than omitting the field for titles it has no runtime for. */
    @Test
    fun testTreatsAZeroRuntimeAsUnknown() {
        assertNull(MovieDetail(runtime = 0).runtimeHoursAndMinutes())
    }

    @Test
    fun testRuntimeIsNullWhenTmdbHasNone() {
        assertNull(MovieDetail().runtimeHoursAndMinutes())
    }

    private companion object {
        /** Any region that is not [RegionConstant.US], so the fallback order is actually exercised. */
        const val VIEWER_REGION = "GB"
    }
}
