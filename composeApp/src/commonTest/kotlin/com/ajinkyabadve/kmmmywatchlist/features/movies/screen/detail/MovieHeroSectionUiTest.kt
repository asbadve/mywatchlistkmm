package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.constant.RegionConstant
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.HeroTestConstant
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.HeroTestSurface
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.RegionWatchProviders
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDateItem
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDatesResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvider
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvidersResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class MovieHeroSectionUiTest {
    /**
     * Providers live under US only, so the resolver's US fallback makes these assertions hold
     * whatever region the machine running the tests reports.
     */
    private fun usProviders(vararg names: String) =
        WatchProvidersResponse(
            results =
                mapOf(
                    RegionConstant.US to
                        RegionWatchProviders(
                            link = HeroTestConstant.MOVIE_WATCH_PAGE_LINK,
                            flatrate = names.mapIndexed { index, name -> WatchProvider(providerId = index, providerName = name) },
                        ),
                ),
        )

    private fun movie(
        title: String = DEFAULT_TITLE,
        releaseDate: String = "2010-07-16",
        runtime: Int? = 148,
        voteAverage: Double = 8.4,
        certification: String? = "PG-13",
        watchProviders: WatchProvidersResponse? = null,
        videos: VideoResponse? = null,
    ) = MovieDetail(
        id = 1,
        title = title,
        releaseDate = releaseDate,
        runtime = runtime,
        voteAverage = voteAverage,
        watchProviders = watchProviders,
        videos = videos,
        releaseDates =
            certification?.let {
                ReleaseDatesResponse(
                    results =
                        listOf(
                            ReleaseDatesResult(
                                iso3166 = RegionConstant.US,
                                releaseDates = listOf(ReleaseDateItem(certification = it)),
                            ),
                        ),
                )
            },
    )

    private fun trailer() =
        VideoResponse(
            results =
                listOf(
                    VideoResult(
                        key = HeroTestConstant.TRAILER_KEY,
                        site = HeroTestConstant.YOUTUBE_SITE,
                        type = HeroTestConstant.TRAILER_TYPE,
                    ),
                ),
        )

    @Test
    fun testRendersTitleAndTheFactsRow() =
        runComposeUiTest {
            setContent { MovieHeroSection(detail = movie(title = "Inception")) }

            onNodeWithText("Inception").assertExists()
            onNodeWithText("2010", substring = true).assertExists()
            onNodeWithText("2h 28m", substring = true).assertExists()
            onNodeWithText("8.4", substring = true).assertExists()
            onNodeWithText("PG-13").assertExists()
        }

    /** Under an hour the hero drops the hours entirely rather than printing "0h". */
    @Test
    fun testShortRuntimesRenderWithoutAnHoursPart() =
        runComposeUiTest {
            setContent { MovieHeroSection(detail = movie(runtime = 45)) }

            onNodeWithText("45m", substring = true).assertExists()
            onAllNodesWithText("0h", substring = true).assertCountEquals(0)
        }

    /** Missing facts are dropped, not rendered as empty separators or zeroes. */
    @Test
    fun testOmitsFactsTmdbHasNoDataFor() =
        runComposeUiTest {
            setContent {
                MovieHeroSection(detail = movie(releaseDate = "", runtime = null, voteAverage = 0.0, certification = null))
            }

            onNodeWithText(DEFAULT_TITLE).assertExists()
            onAllNodesWithText("0.0", substring = true).assertCountEquals(0)
            // The star prefixes the rating and the separator joins facts; neither can appear when
            // there are no facts to show.
            onAllNodesWithText(HeroTestConstant.RATING_STAR, substring = true).assertCountEquals(0)
            onAllNodesWithText(HeroTestConstant.FACT_SEPARATOR, substring = true).assertCountEquals(0)
        }

    @Test
    fun testNamesTheStreamingServiceOnThePrimaryButton() =
        runComposeUiTest {
            setContent { MovieHeroSection(detail = movie(watchProviders = usProviders(HeroTestConstant.PROVIDER_NETFLIX))) }

            onNodeWithText(HeroTestConstant.WATCH_ON_NETFLIX).assertExists()
        }

    /** The button promises a destination, so it must open TMDB's watch page for the title. */
    @Test
    fun testWatchButtonOpensTheWatchPage() =
        runComposeUiTest {
            var opened: String? = null
            setContent {
                HeroTestSurface {
                    MovieHeroSection(
                        detail = movie(watchProviders = usProviders(HeroTestConstant.PROVIDER_NETFLIX)),
                        onOpenUrl = { opened = it },
                    )
                }
            }

            onNodeWithText(HeroTestConstant.WATCH_ON_NETFLIX).performClick()

            assertEquals(HeroTestConstant.MOVIE_WATCH_PAGE_LINK, opened)
        }

    /** Beyond two, the chips wrap and start competing with the button they sit above. */
    @Test
    fun testShowsAtMostTwoProviderChips() =
        runComposeUiTest {
            setContent {
                MovieHeroSection(
                    detail = movie(watchProviders = usProviders(HeroTestConstant.PROVIDER_NETFLIX, "Prime Video", "Disney+")),
                )
            }

            onNodeWithText(HeroTestConstant.PROVIDER_NETFLIX).assertExists()
            onNodeWithText("Prime Video").assertExists()
            onAllNodesWithText("Disney+").assertCountEquals(0)
        }

    /**
     * With nowhere to stream it, the trailer is all that is left to offer, so it takes the labelled
     * slot rather than leaving the row as one unexplained icon.
     */
    @Test
    fun testTrailerBecomesTheLabelledActionWhenNothingStreamsIt() =
        runComposeUiTest {
            var opened: String? = null
            setContent { HeroTestSurface { MovieHeroSection(detail = movie(videos = trailer()), onOpenUrl = { opened = it }) } }

            onNodeWithText(HeroTestConstant.PLAY_TRAILER).performClick()

            assertEquals(HeroTestConstant.TRAILER_URL, opened)
        }

    /** Watching the thing beats watching an advert for it, so the trailer demotes to an icon. */
    @Test
    fun testTrailerDemotesToAnIconWhenThereIsSomewhereToWatch() =
        runComposeUiTest {
            var opened: String? = null
            setContent {
                HeroTestSurface {
                    MovieHeroSection(
                        detail = movie(watchProviders = usProviders(HeroTestConstant.PROVIDER_NETFLIX), videos = trailer()),
                        onOpenUrl = { opened = it },
                    )
                }
            }

            onAllNodesWithText(HeroTestConstant.PLAY_TRAILER).assertCountEquals(0)
            onNodeWithContentDescription(HeroTestConstant.PLAY_TRAILER).performClick()

            assertEquals(HeroTestConstant.TRAILER_URL, opened)
        }

    /** Neither a provider nor a trailer means no action row at all - not an empty strip of buttons. */
    @Test
    fun testDrawsNoActionsWhenThereIsNothingToOpen() =
        runComposeUiTest {
            setContent { MovieHeroSection(detail = movie()) }

            onAllNodesWithText(HeroTestConstant.PLAY_TRAILER).assertCountEquals(0)
            onAllNodesWithText(HeroTestConstant.WATCH_ON_PREFIX, substring = true).assertCountEquals(0)
        }

    /** A provider named on the button that opens nothing would be worse than no button. */
    @Test
    fun testRegionWithNoProvidersDrawsNoWatchButton() =
        runComposeUiTest {
            var opened: String? = null
            setContent {
                MovieHeroSection(
                    detail = movie(watchProviders = WatchProvidersResponse(results = emptyMap())),
                    onOpenUrl = { opened = it },
                )
            }

            onAllNodesWithText(HeroTestConstant.WATCH_ON_PREFIX, substring = true).assertCountEquals(0)
            assertNull(opened)
        }

    private companion object {
        /** Asserted on where the title itself is not what the case is about. */
        const val DEFAULT_TITLE = "A Film"
    }
}
