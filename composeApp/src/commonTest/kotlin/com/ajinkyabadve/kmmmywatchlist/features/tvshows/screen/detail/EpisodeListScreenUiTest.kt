package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
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
class EpisodeListScreenUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun successfulSeasonWithEpisodes() =
        TvSeasonDetail(
            id = 1,
            seasonNumber = 1,
            name = "Season 1",
            episodes = listOf(Episode(id = 101, name = "Pilot", episodeNumber = 1, seasonNumber = 1)),
        )

    @Test
    fun testEpisodeListScreen_errorState_showsMessageAndRetrySucceeds() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getSeasonDetailsResult = Result.failure(IOException("boom"))
                }
            val viewModel = EpisodeListScreenModel(tvId = 1, seasonNumber = 1, tvRepository = fakeRepository)

            setContent {
                EpisodeListScreen(
                    tvShowId = 1,
                    seasonNumber = 1,
                    onBackClicked = {},
                    onEpisodeClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Network Connection Error. Please check your internet connectivity.").assertExists()

            fakeRepository.getSeasonDetailsResult = Result.success(successfulSeasonWithEpisodes())
            onNodeWithText("Retry").performClick()

            onNodeWithText("1. Pilot").assertExists()
        }

    @Test
    fun testEpisodeListScreen_noEpisodes_showsEmptyMessage() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getSeasonDetailsResult = Result.success(TvSeasonDetail(id = 1, seasonNumber = 1, name = "Season 1"))
                }
            val viewModel = EpisodeListScreenModel(tvId = 1, seasonNumber = 1, tvRepository = fakeRepository)

            setContent {
                EpisodeListScreen(
                    tvShowId = 1,
                    seasonNumber = 1,
                    onBackClicked = {},
                    onEpisodeClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("No episodes available for this season.").assertExists()
        }

    @Test
    fun testEpisodeListScreen_episodeClick_invokesOnEpisodeClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getSeasonDetailsResult = Result.success(successfulSeasonWithEpisodes())
                }
            val viewModel = EpisodeListScreenModel(tvId = 1, seasonNumber = 1, tvRepository = fakeRepository)
            var clickedEpisodeNumber: Int? = null

            setContent {
                EpisodeListScreen(
                    tvShowId = 1,
                    seasonNumber = 1,
                    onBackClicked = {},
                    onEpisodeClicked = { clickedEpisodeNumber = it },
                    viewModel = viewModel,
                )
            }

            onNodeWithText("1. Pilot").performClick()
            assertEquals(1, clickedEpisodeNumber)
        }

    @Test
    fun testEpisodeListScreen_backClicked_invokesOnBackClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getSeasonDetailsResult = Result.success(successfulSeasonWithEpisodes())
                }
            val viewModel = EpisodeListScreenModel(tvId = 1, seasonNumber = 1, tvRepository = fakeRepository)
            var backClicked = false

            setContent {
                EpisodeListScreen(
                    tvShowId = 1,
                    seasonNumber = 1,
                    onBackClicked = { backClicked = true },
                    onEpisodeClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithContentDescription("Back").performClick()
            assertEquals(true, backClicked)
        }
}
