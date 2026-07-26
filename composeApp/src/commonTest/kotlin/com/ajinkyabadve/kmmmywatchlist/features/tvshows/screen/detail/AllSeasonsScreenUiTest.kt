package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.SeasonSummary
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
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
class AllSeasonsScreenUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun successfulTvDetailWithSeasons() =
        TvDetail(
            id = 1,
            title = "Fake Tv Detail",
            seasons =
                listOf(
                    SeasonSummary(id = 11, name = "Season 1", seasonNumber = 1, episodeCount = 10),
                ),
        )

    @Test
    fun testAllSeasonsScreen_errorState_showsMessageAndRetrySucceeds() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.failure(IOException("boom"))
                }
            val viewModel = AllSeasonsScreenModel(tvId = 1, tvRepository = fakeRepository)

            setContent {
                AllSeasonsScreen(
                    tvShowId = 1,
                    onBackClicked = {},
                    onSeasonClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Network Connection Error. Please check your internet connectivity.").assertExists()

            fakeRepository.getTvDetailsResult = Result.success(successfulTvDetailWithSeasons())
            onNodeWithText("Retry").performClick()

            onNodeWithText("Season 1").assertExists()
        }

    @Test
    fun testAllSeasonsScreen_seasonClick_invokesOnSeasonClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(successfulTvDetailWithSeasons())
                }
            val viewModel = AllSeasonsScreenModel(tvId = 1, tvRepository = fakeRepository)
            var clickedSeasonNumber: Int? = null

            setContent {
                AllSeasonsScreen(
                    tvShowId = 1,
                    onBackClicked = {},
                    onSeasonClicked = { clickedSeasonNumber = it },
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Season 1").performClick()
            assertEquals(1, clickedSeasonNumber)
        }

    @Test
    fun testAllSeasonsScreen_backClicked_invokesOnBackClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getTvDetailsResult = Result.success(successfulTvDetailWithSeasons())
                }
            val viewModel = AllSeasonsScreenModel(tvId = 1, tvRepository = fakeRepository)
            var backClicked = false

            setContent {
                AllSeasonsScreen(
                    tvShowId = 1,
                    onBackClicked = { backClicked = true },
                    onSeasonClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithContentDescription("Back").performClick()
            assertEquals(true, backClicked)
        }
}
