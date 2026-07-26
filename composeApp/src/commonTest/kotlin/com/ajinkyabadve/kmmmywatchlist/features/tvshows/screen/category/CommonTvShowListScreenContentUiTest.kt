package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowsConstant
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
class CommonTvShowListScreenContentUiTest {
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
    fun testTvShowScreenContent_tvShowClick_invokesOnTvShowSelected() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvShowsResult =
                        Result.success(
                            TvPageResult(
                                page = 1,
                                list = listOf(Tv(id = 201, title = "Popular Tv Show")),
                                totalResults = 1,
                                totalPages = 1,
                            ),
                        )
                }
            val viewModel = TvListScreenModel(TvShowsConstant.POPULAR_API_PATH, fakeRepository)
            var selectedTvShowId: Long? = null

            setContent {
                tvShowScreenContent(viewModel = viewModel, onTvShowSelected = { selectedTvShowId = it })
            }

            onAllNodesWithText("Popular Tv Show")[0].performClick()
            assertEquals(201L, selectedTvShowId)
        }

    @Test
    fun testTvShowScreenContent_networkError_showsRetryThatReloads() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvShowsResult = Result.failure(IOException("boom"))
                }
            val viewModel = TvListScreenModel(TvShowsConstant.POPULAR_API_PATH, fakeRepository)

            setContent {
                tvShowScreenContent(viewModel = viewModel)
            }

            onNodeWithText("Retry").assertExists()

            fakeRepository.getTvShowsResult =
                Result.success(
                    TvPageResult(
                        page = 1,
                        list = listOf(Tv(id = 202, title = "Recovered Tv Show")),
                        totalResults = 1,
                        totalPages = 1,
                    ),
                )
            onNodeWithText("Retry").performClick()

            onAllNodesWithText("Recovered Tv Show")[0].assertExists()
            assertTrue(fakeRepository.getTvShowsCalls.size >= 2)
        }
}
