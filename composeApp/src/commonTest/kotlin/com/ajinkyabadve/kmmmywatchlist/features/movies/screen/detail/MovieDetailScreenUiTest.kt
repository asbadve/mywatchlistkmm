package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionInfo
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Credits
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
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
class MovieDetailScreenUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun successfulMovieDetail() =
        MovieDetail(
            id = 1,
            title = "Fake Movie Detail",
            tagline = "Every story has an end.",
            overview = "Overview of Fake Movie Detail",
            releaseDate = "2026-07-02",
            credits =
                Credits(
                    cast = listOf(CastMember(id = 301, name = "Actor One", character = "Hero", order = 0)),
                ),
            recommendations =
                MoviePageResult(
                    page = 1,
                    list = listOf(Movie(id = 201, title = "Rec Movie X")),
                    totalResults = 1,
                    totalPages = 1,
                ),
            similar =
                MoviePageResult(
                    page = 1,
                    list = listOf(Movie(id = 202, title = "Similar Movie Y")),
                    totalResults = 1,
                    totalPages = 1,
                ),
            belongsToCollection = CollectionInfo(id = 501, name = "Infinity Saga"),
        )

    @Test
    fun testMovieDetailScreen_errorState_showsMessageAndRetrySucceeds() =
        runComposeUiTest {
            val fakeRepository =
                FakeMovieRepository().apply {
                    getMovieDetailsResult = Result.failure(IOException("boom"))
                }
            val viewModel = MovieDetailScreenModel(movieId = 1, movieRepository = fakeRepository)

            setContent {
                MovieDetailScreen(
                    movieId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Network Connection Error. Please check your internet connectivity.").assertExists()

            // Retry with a now-successful repository response.
            fakeRepository.getMovieDetailsResult = Result.success(successfulMovieDetail())
            onNodeWithText("Retry").performClick()

            onNodeWithText("Fake Movie Detail").assertExists()
        }

    @Test
    fun testMovieDetailScreen_success_rendersTitleTaglineAndCollectionBanner() =
        runComposeUiTest {
            val fakeRepository =
                FakeMovieRepository().apply {
                    getMovieDetailsResult = Result.success(successfulMovieDetail())
                }
            val viewModel = MovieDetailScreenModel(movieId = 1, movieRepository = fakeRepository)

            setContent {
                MovieDetailScreen(
                    movieId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    viewModel = viewModel,
                )
            }

            // The title is in the hero, which fills most of the first screenful; the tagline and
            // collection banner sit below it, so the lazy list has to be scrolled before they exist.
            onNodeWithText("Fake Movie Detail").assertExists()
            onNode(hasScrollAction()).performScrollToNode(hasText("Every story has an end."))
            onNodeWithText("Every story has an end.").assertExists()
            onNode(hasScrollAction()).performScrollToNode(hasText("View collection", substring = true))
            onNodeWithText("View collection", substring = true).assertExists()
        }

    @Test
    fun testMovieDetailScreen_collectionBannerClick_invokesOnCollectionClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeMovieRepository().apply {
                    getMovieDetailsResult = Result.success(successfulMovieDetail())
                }
            val viewModel = MovieDetailScreenModel(movieId = 1, movieRepository = fakeRepository)
            var collectionId: Long? = null

            setContent {
                MovieDetailScreen(
                    movieId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    onCollectionClicked = { collectionId = it },
                    viewModel = viewModel,
                )
            }

            onNode(hasScrollAction()).performScrollToNode(hasText("View collection", substring = true))
            onNodeWithText("View collection", substring = true).performClick()
            assertEquals(501L, collectionId)
        }

    @Test
    fun testMovieDetailScreen_castMemberClick_invokesOnPersonClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeMovieRepository().apply {
                    getMovieDetailsResult = Result.success(successfulMovieDetail())
                }
            val viewModel = MovieDetailScreenModel(movieId = 1, movieRepository = fakeRepository)
            var personId: Long? = null

            setContent {
                MovieDetailScreen(
                    movieId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = {},
                    onPersonClicked = { personId = it },
                    viewModel = viewModel,
                )
            }

            onNode(hasScrollToIndexAction()).performScrollToIndex(6)
            onNodeWithText("Actor One").performClick()
            assertEquals(301L, personId)
        }

    @Test
    fun testMovieDetailScreen_recommendationClick_invokesOnMovieClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeMovieRepository().apply {
                    getMovieDetailsResult = Result.success(successfulMovieDetail())
                }
            val viewModel = MovieDetailScreenModel(movieId = 1, movieRepository = fakeRepository)
            var clickedMovieId: Long? = null

            setContent {
                MovieDetailScreen(
                    movieId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = { clickedMovieId = it },
                    viewModel = viewModel,
                )
            }

            onNode(hasScrollToIndexAction()).performScrollToIndex(7)
            onNodeWithText("Rec Movie X").performClick()
            assertEquals(201L, clickedMovieId)
        }

    @Test
    fun testMovieDetailScreen_similarMovieClick_invokesOnMovieClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeMovieRepository().apply {
                    getMovieDetailsResult = Result.success(successfulMovieDetail())
                }
            val viewModel = MovieDetailScreenModel(movieId = 1, movieRepository = fakeRepository)
            var clickedMovieId: Long? = null

            setContent {
                MovieDetailScreen(
                    movieId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onMovieClicked = { clickedMovieId = it },
                    viewModel = viewModel,
                )
            }

            onNode(hasScrollToIndexAction()).performScrollToIndex(8)
            onNodeWithText("Similar Movie Y").performClick()
            assertEquals(202L, clickedMovieId)
        }

    @Test
    fun testMovieDetailScreen_backClicked_invokesOnBackClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeMovieRepository().apply {
                    getMovieDetailsResult = Result.success(successfulMovieDetail())
                }
            val viewModel = MovieDetailScreenModel(movieId = 1, movieRepository = fakeRepository)
            var backClicked = false

            setContent {
                MovieDetailScreen(
                    movieId = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = { backClicked = true },
                    onMovieClicked = {},
                    viewModel = viewModel,
                )
            }

            // "Back", not "Close": the detail screens now share one DetailTopBar, so the
            // back affordance is the same here as on every other detail screen.
            onNodeWithContentDescription("Back").performClick()
            assertEquals(true, backClicked)
        }
}
