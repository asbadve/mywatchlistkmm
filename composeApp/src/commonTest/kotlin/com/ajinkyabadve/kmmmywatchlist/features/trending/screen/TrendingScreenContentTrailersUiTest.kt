package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class TrendingScreenContentTrailersUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // A ViewModel with a trailer already loaded, so the only thing under test is whether the
    // showTrailers flag decides to render the rail.
    private fun viewModelWithTrailer(): TrendingScreenTabViewModel {
        val movieRepository =
            FakeMovieRepository().apply {
                getMoviesResult =
                    Result.success(
                        MoviePageResult(page = 1, list = listOf(Movie(id = 9, title = "Movie")), totalResults = 1, totalPages = 1),
                    )
                getMovieVideosResults[9L] =
                    Result.success(
                        VideoResponse(
                            results =
                                listOf(
                                    VideoResult(
                                        id = "v",
                                        key = "k",
                                        site = "YouTube",
                                        type = "Trailer",
                                        official = true,
                                        publishedAt = "2026-01-01T00:00:00.000Z",
                                    ),
                                ),
                        ),
                    )
            }
        return TrendingScreenTabViewModel(
            FakeTrendingRepository(),
            movieRepository,
            FakeTvRepository(),
            trailersEnabled = true,
        )
    }

    @Test
    fun testTrailersHidden_whenShowTrailersFalse() =
        runComposeUiTest {
            setContent {
                TrendingScreenContent(
                    screenLoadingState = false,
                    sections = emptyList(),
                    onChipSelected = { _, _ -> },
                    onMovieSelected = {},
                    trailersViewModel = viewModelWithTrailer(),
                    showTrailers = false,
                )
            }

            onNodeWithText("Latest Trailers").assertDoesNotExist()
        }

    @Test
    fun testTrailersShown_whenShowTrailersTrue() =
        runComposeUiTest {
            setContent {
                TrendingScreenContent(
                    screenLoadingState = false,
                    sections = emptyList(),
                    onChipSelected = { _, _ -> },
                    onMovieSelected = {},
                    trailersViewModel = viewModelWithTrailer(),
                    showTrailers = true,
                )
            }

            onNodeWithText("Latest Trailers").assertIsDisplayed()
        }
}
