package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.constant.RegionConstant
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.HeroTestConstant
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.HeroTestSurface
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.RegionWatchProviders
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvider
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvidersResponse
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.ContentRating
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.ContentRatingsResponse
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Network
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TvHeroSectionUiTest {
    /** US-only, so the resolver's fallback keeps these assertions independent of the machine's region. */
    private fun usProviders(name: String) =
        WatchProvidersResponse(
            results =
                mapOf(
                    RegionConstant.US to
                        RegionWatchProviders(
                            link = HeroTestConstant.TV_WATCH_PAGE_LINK,
                            flatrate = listOf(WatchProvider(providerId = 1, providerName = name)),
                        ),
                ),
        )

    private fun show(
        title: String = "A Series",
        status: String? = TvStatusTestConstant.RETURNING,
        firstAirDate: String = "2011-04-17",
        numberOfSeasons: Int? = 8,
        numberOfEpisodes: Int? = 73,
        voteAverage: Double = 8.4,
        contentRating: String? = "TV-MA",
        network: String? = "HBO",
        nextEpisodeToAir: Episode? = null,
        lastEpisodeToAir: Episode? = null,
        watchProviders: WatchProvidersResponse? = null,
        videos: VideoResponse? = null,
    ) = TvDetail(
        id = 1,
        title = title,
        status = status,
        firstAirDate = firstAirDate,
        numberOfSeasons = numberOfSeasons,
        numberOfEpisodes = numberOfEpisodes,
        voteAverage = voteAverage,
        networks = network?.let { listOf(Network(id = 1, name = it)) },
        nextEpisodeToAir = nextEpisodeToAir,
        lastEpisodeToAir = lastEpisodeToAir,
        watchProviders = watchProviders,
        videos = videos,
        contentRatings =
            contentRating?.let {
                ContentRatingsResponse(results = listOf(ContentRating(iso3166 = RegionConstant.US, rating = it)))
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
    fun testRendersTitleAndTheSeriesFacts() =
        runComposeUiTest {
            setContent { TvHeroSection(detail = show(title = "Game of Thrones")) {} }

            onNodeWithText("Game of Thrones").assertExists()
            onNodeWithText("2011", substring = true).assertExists()
            onNodeWithText("8 Seasons", substring = true).assertExists()
            onNodeWithText("73 Episodes", substring = true).assertExists()
            onNodeWithText("TV-MA").assertExists()
            onNodeWithText("HBO").assertExists()
        }

    /** A one-season show reads "1 Season", not "1 Seasons". */
    @Test
    fun testASingleSeasonIsNotPluralised() =
        runComposeUiTest {
            setContent { TvHeroSection(detail = show(numberOfSeasons = 1)) {} }

            onNodeWithText("1 Season", substring = true).assertExists()
            onAllNodesWithText("1 Seasons", substring = true).assertCountEquals(0)
        }

    /**
     * Whether a show is still running is the fact a film never needs, and it is shown with TMDB's
     * own wording rather than a generic "Ongoing".
     */
    @Test
    fun testARunningShowIsBadgedWithItsTmdbStatus() =
        runComposeUiTest {
            setContent {
                TvHeroSection(detail = show(status = TvStatusTestConstant.RETURNING)) {}
            }

            onNodeWithText(TvStatusTestConstant.RETURNING).assertExists()
        }

    @Test
    fun testAFinishedShowCarriesNoBadge() =
        runComposeUiTest {
            setContent { TvHeroSection(detail = show(status = TvStatusTestConstant.ENDED)) {} }

            onAllNodesWithText(TvStatusTestConstant.ENDED).assertCountEquals(0)
        }

    /** A returning series is best entered at what is coming, so the next episode wins the line. */
    @Test
    fun testNamesTheNextEpisodeWhenOneIsScheduled() =
        runComposeUiTest {
            setContent {
                TvHeroSection(
                    detail =
                        show(
                            nextEpisodeToAir = Episode(id = 2, name = "What Comes Next", seasonNumber = 3, episodeNumber = 4),
                            lastEpisodeToAir = Episode(id = 1, name = "What Just Aired", seasonNumber = 3, episodeNumber = 3),
                        ),
                ) {}
            }

            onNodeWithText("What Comes Next", substring = true).assertExists()
            onNodeWithText("S3 E4", substring = true).assertExists()
            onAllNodesWithText("What Just Aired", substring = true).assertCountEquals(0)
        }

    /** A finished show has no "next", so the last episode is the honest thing to name. */
    @Test
    fun testFallsBackToTheLastAiredEpisode() =
        runComposeUiTest {
            setContent {
                TvHeroSection(
                    detail =
                        show(
                            status = TvStatusTestConstant.ENDED,
                            lastEpisodeToAir = Episode(id = 1, name = "The Finale", seasonNumber = 6, episodeNumber = 10),
                        ),
                ) {}
            }

            onNodeWithText("The Finale", substring = true).assertExists()
        }

    @Test
    fun testWatchButtonOpensTheWatchPage() =
        runComposeUiTest {
            var opened: String? = null
            setContent {
                HeroTestSurface {
                    TvHeroSection(
                        detail = show(watchProviders = usProviders(HeroTestConstant.PROVIDER_NETFLIX)),
                        onOpenUrl = { opened = it },
                    ) {}
                }
            }

            onNodeWithText(HeroTestConstant.WATCH_ON_NETFLIX).performClick()

            assertEquals(HeroTestConstant.TV_WATCH_PAGE_LINK, opened)
        }

    @Test
    fun testTrailerBecomesTheLabelledActionWhenNothingStreamsIt() =
        runComposeUiTest {
            var opened: String? = null
            setContent {
                HeroTestSurface {
                    TvHeroSection(detail = show(videos = trailer()), onOpenUrl = { opened = it }) {}
                }
            }

            onNodeWithText(HeroTestConstant.PLAY_TRAILER).performClick()

            assertEquals(HeroTestConstant.TRAILER_URL, opened)
        }

    @Test
    fun testTrailerDemotesToAnIconWhenThereIsSomewhereToWatch() =
        runComposeUiTest {
            var opened: String? = null
            setContent {
                HeroTestSurface {
                    TvHeroSection(
                        detail = show(watchProviders = usProviders(HeroTestConstant.PROVIDER_NETFLIX), videos = trailer()),
                        onOpenUrl = { opened = it },
                    ) {}
                }
            }

            onAllNodesWithText(HeroTestConstant.PLAY_TRAILER).assertCountEquals(0)
            onNodeWithContentDescription(HeroTestConstant.PLAY_TRAILER).performClick()

            assertEquals(HeroTestConstant.TRAILER_URL, opened)
        }

    @Test
    fun testDrawsNoActionsWhenThereIsNothingToOpen() =
        runComposeUiTest {
            setContent { TvHeroSection(detail = show()) {} }

            onAllNodesWithText(HeroTestConstant.PLAY_TRAILER).assertCountEquals(0)
            onAllNodesWithText(HeroTestConstant.WATCH_ON_PREFIX, substring = true).assertCountEquals(0)
        }

    /**
     * The TV hero lost the most in light theme - the network line, the next-episode line and both
     * provider chips all sat in the band that turned white under white text. Driven at an explicit
     * light theme so it does not depend on the test machine's system setting.
     *
     * Node existence is all this can assert; `HeroColorsTest` is what holds the contrast.
     */
    @Test
    fun testRendersTheWholeHeroInLightTheme() =
        runComposeUiTest {
            setContent {
                AppTheme(useDarkTheme = false) {
                    HeroTestSurface {
                        TvHeroSection(
                            detail =
                                show(
                                    title = "Game of Thrones",
                                    watchProviders = usProviders(HeroTestConstant.PROVIDER_NETFLIX),
                                    nextEpisodeToAir = Episode(id = 2, name = "What Comes Next", seasonNumber = 3, episodeNumber = 4),
                                ),
                        ) {}
                    }
                }
            }

            onNodeWithText("Game of Thrones").assertIsDisplayed()
            onNodeWithText(TvStatusTestConstant.RETURNING).assertIsDisplayed()
            onNodeWithText("TV-MA").assertIsDisplayed()
            onNodeWithText("HBO").assertIsDisplayed()
            onNodeWithText("What Comes Next", substring = true).assertIsDisplayed()
            onNodeWithText(HeroTestConstant.PROVIDER_NETFLIX).assertIsDisplayed()
            onNodeWithText(HeroTestConstant.WATCH_ON_NETFLIX).assertIsDisplayed()
        }
}
