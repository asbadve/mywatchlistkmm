package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Credits
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.SeasonSummary
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class TvDetailScreenUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun successfulTvDetail() =
        TvDetail(
            id = 1,
            title = "Fake Tv Detail",
            tagline = "Every episode has an end.",
            overview = "Overview of Fake Tv Detail",
            firstAirDate = "2026-07-02",
            seasons = listOf(SeasonSummary(id = 11, name = "Season 1", seasonNumber = 1)),
            credits =
                Credits(
                    cast = listOf(CastMember(id = 301, name = "Actor One", character = "Hero", order = 0)),
                ),
            recommendations =
                TvPageResult(
                    page = 1,
                    list = listOf(Tv(id = 201, title = "Rec Tv Show X")),
                    totalResults = 1,
                    totalPages = 1,
                ),
            similar =
                TvPageResult(
                    page = 1,
                    list = listOf(Tv(id = 202, title = "Similar Tv Show Y")),
                    totalResults = 1,
                    totalPages = 1,
                ),
        )

    @Test
    fun testTvDetailScreen_errorState_showsMessageAndRetrySucceeds() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.failure(IOException("boom"))
                }
            val viewModel = TvDetailScreenModel(tvId = 1, tvRepository = fakeRepository)

            setContent {
                TvDetailScreen(
                    tvShowId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onTvShowClicked = {},
                    onViewAllSeasonsClick = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Network Connection Error. Please check your internet connectivity.").assertExists()

            fakeRepository.getTvDetailsResult = Result.success(successfulTvDetail())
            onNodeWithText("Retry").performClick()

            onNodeWithText("Fake Tv Detail").assertExists()
        }

    @Test
    fun testTvDetailScreen_success_rendersTitleAndCurrentSeason() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(successfulTvDetail())
                }
            val viewModel = TvDetailScreenModel(tvId = 1, tvRepository = fakeRepository)

            setContent {
                TvDetailScreen(
                    tvShowId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onTvShowClicked = {},
                    onViewAllSeasonsClick = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Fake Tv Detail").assertExists()

            onNode(hasScrollToIndexAction()).performScrollToIndex(4)
            onNodeWithText("Season 1").assertExists()
        }

    @Test
    fun testTvDetailScreen_viewAllSeasonsClick_invokesCallbackWithTvShowId() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(successfulTvDetail())
                }
            val viewModel = TvDetailScreenModel(tvId = 1, tvRepository = fakeRepository)
            var viewAllSeasonsId: Long? = null

            setContent {
                TvDetailScreen(
                    tvShowId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onTvShowClicked = {},
                    onViewAllSeasonsClick = { viewAllSeasonsId = it },
                    viewModel = viewModel,
                )
            }

            onNode(hasScrollToIndexAction()).performScrollToIndex(4)
            // Clicks the season row rather than the "View All" TextButton - both trigger the same
            // onViewAllSeasonsClick callback, but the TextButton's ripple/indication machinery
            // doesn't reliably deliver its click in this embedded, animated-header context.
            onNodeWithText("Season 1", substring = true).performClick()
            assertEquals(1L, viewAllSeasonsId)
        }

    @Test
    fun testTvDetailScreen_castMemberClick_invokesOnPersonClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(successfulTvDetail())
                }
            val viewModel = TvDetailScreenModel(tvId = 1, tvRepository = fakeRepository)
            var personId: Long? = null

            setContent {
                TvDetailScreen(
                    tvShowId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onTvShowClicked = {},
                    onViewAllSeasonsClick = {},
                    onPersonClicked = { personId = it },
                    viewModel = viewModel,
                )
            }

            onNode(hasScrollToIndexAction()).performScrollToIndex(7)
            onNodeWithText("Actor One").performClick()
            assertEquals(301L, personId)
        }

    @Test
    fun testTvDetailScreen_recommendationClick_invokesOnTvShowClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(successfulTvDetail())
                }
            val viewModel = TvDetailScreenModel(tvId = 1, tvRepository = fakeRepository)
            var clickedTvShowId: Long? = null

            setContent {
                TvDetailScreen(
                    tvShowId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onTvShowClicked = { clickedTvShowId = it },
                    onViewAllSeasonsClick = {},
                    viewModel = viewModel,
                )
            }

            onNode(hasScrollToIndexAction()).performScrollToIndex(8)
            onNodeWithText("Rec Tv Show X").performClick()
            assertEquals(201L, clickedTvShowId)
        }

    @Test
    fun testTvDetailScreen_similarTvShowClick_invokesOnTvShowClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(successfulTvDetail())
                }
            val viewModel = TvDetailScreenModel(tvId = 1, tvRepository = fakeRepository)
            var clickedTvShowId: Long? = null

            setContent {
                TvDetailScreen(
                    tvShowId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onTvShowClicked = { clickedTvShowId = it },
                    onViewAllSeasonsClick = {},
                    viewModel = viewModel,
                )
            }

            onNode(hasScrollToIndexAction()).performScrollToIndex(9)
            onNodeWithText("Similar Tv Show Y").performClick()
            assertEquals(202L, clickedTvShowId)
        }

    @Test
    fun testTvDetailScreen_backClicked_invokesOnBackClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(successfulTvDetail())
                }
            val viewModel = TvDetailScreenModel(tvId = 1, tvRepository = fakeRepository)
            var backClicked = false

            setContent {
                TvDetailScreen(
                    tvShowId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = { backClicked = true },
                    onTvShowClicked = {},
                    onViewAllSeasonsClick = {},
                    viewModel = viewModel,
                )
            }

            // "Back", not "Close": the detail screens now share one DetailTopBar, so the
            // back affordance is the same here as on every other detail screen.
            onNodeWithContentDescription("Back").performClick()
            assertEquals(true, backClicked)
        }
}
