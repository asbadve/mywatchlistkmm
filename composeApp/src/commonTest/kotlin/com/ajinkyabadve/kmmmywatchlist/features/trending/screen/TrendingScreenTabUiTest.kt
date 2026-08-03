package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class TrendingScreenTabUiTest {
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
    fun testTrendingScreenTab_rendersAllThreeSections() =
        runComposeUiTest {
            val fakeTrendingRepository =
                FakeTrendingRepository().apply {
                    getTrendingResult =
                        Result.success(
                            MoviePageResult(
                                page = 1,
                                list = listOf(Movie(id = 1, title = "Trending Movie X", posterPath = "/pathX.jpg")),
                                totalResults = 1,
                                totalPages = 1,
                            ),
                        )
                }
            val viewModel = TrendingScreenTabViewModel(fakeTrendingRepository)

            setContent {
                TrendingScreenTab(viewModel = viewModel)
            }

            onNodeWithText("Trending Movies").assertExists()
            onNodeWithText("Trending Tv show").assertExists()
            onNodeWithText("Trending People").assertExists()
            // Same fake result is returned for all three media types (movie/tv/people).
            onAllNodesWithText("Trending Movie X").assertCountEquals(3)
        }
}
