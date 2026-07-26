package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant
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
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class CommonMovieListScreenContentUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testScreenContent_movieClick_invokesOnMovieSelected() =
        runComposeUiTest {
            val fakeRepository =
                FakeMovieRepository().apply {
                    getMoviesResult =
                        Result.success(
                            MoviePageResult(
                                page = 1,
                                list = listOf(Movie(id = 201, title = "Popular Movie C")),
                                totalResults = 1,
                                totalPages = 1,
                            ),
                        )
                }
            val viewModel = MovieListScreenModel(MoviesConstant.POPULAR_API_PATH, fakeRepository)
            var selectedMovieId: Long? = null

            setContent {
                screenContent(viewModel = viewModel, onMovieSelected = { selectedMovieId = it })
            }

            onAllNodesWithText("Popular Movie C")[0].performClick()
            assertEquals(201L, selectedMovieId)
        }

    @Test
    fun testScreenContent_networkError_showsRetryThatReloads() =
        runComposeUiTest {
            val fakeRepository =
                FakeMovieRepository().apply {
                    getMoviesResult = Result.failure(IOException("boom"))
                }
            val viewModel = MovieListScreenModel(MoviesConstant.POPULAR_API_PATH, fakeRepository)

            setContent {
                screenContent(viewModel = viewModel)
            }

            onNodeWithText("Retry").assertExists()

            fakeRepository.getMoviesResult =
                Result.success(
                    MoviePageResult(
                        page = 1,
                        list = listOf(Movie(id = 202, title = "Recovered Movie")),
                        totalResults = 1,
                        totalPages = 1,
                    ),
                )
            onNodeWithText("Retry").performClick()

            onAllNodesWithText("Recovered Movie")[0].assertExists()
            assertTrue(fakeRepository.getMoviesCalls.size >= 2)
        }
}
