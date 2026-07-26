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
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.EpisodeCredits
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.EpisodeDetail
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
class EpisodeDetailScreenUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun successfulEpisodeDetail() =
        EpisodeDetail(
            id = 1,
            name = "Pilot",
            episodeNumber = 1,
            seasonNumber = 1,
            credits =
                EpisodeCredits(
                    cast = listOf(CastMember(id = 301, name = "Actor One", character = "Hero", order = 0)),
                    guestStars = listOf(CastMember(id = 302, name = "Guest One", character = "Cameo", order = 0)),
                ),
        )

    @Test
    fun testEpisodeDetailScreen_errorState_showsMessageAndRetrySucceeds() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getEpisodeDetailsResult = Result.failure(IOException("boom"))
                }
            val viewModel = EpisodeDetailScreenModel(tvId = 1, seasonNumber = 1, episodeNumber = 1, tvRepository = fakeRepository)

            setContent {
                EpisodeDetailScreen(
                    tvShowId = 1,
                    seasonNumber = 1,
                    episodeNumber = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Network Connection Error. Please check your internet connectivity.").assertExists()

            fakeRepository.getEpisodeDetailsResult = Result.success(successfulEpisodeDetail())
            onNodeWithText("Retry").performClick()

            onNodeWithText("Episode 1 • Season 1").assertExists()
        }

    @Test
    fun testEpisodeDetailScreen_castClick_invokesOnPersonClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getEpisodeDetailsResult = Result.success(successfulEpisodeDetail())
                }
            val viewModel = EpisodeDetailScreenModel(tvId = 1, seasonNumber = 1, episodeNumber = 1, tvRepository = fakeRepository)
            var personId: Long? = null

            setContent {
                EpisodeDetailScreen(
                    tvShowId = 1,
                    seasonNumber = 1,
                    episodeNumber = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onPersonClicked = { personId = it },
                    viewModel = viewModel,
                )
            }

            onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(4)
            onNodeWithText("Actor One").performClick()
            assertEquals(301L, personId)
        }

    @Test
    fun testEpisodeDetailScreen_guestStarClick_invokesOnPersonClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getEpisodeDetailsResult = Result.success(successfulEpisodeDetail())
                }
            val viewModel = EpisodeDetailScreenModel(tvId = 1, seasonNumber = 1, episodeNumber = 1, tvRepository = fakeRepository)
            var personId: Long? = null

            setContent {
                EpisodeDetailScreen(
                    tvShowId = 1,
                    seasonNumber = 1,
                    episodeNumber = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = {},
                    onPersonClicked = { personId = it },
                    viewModel = viewModel,
                )
            }

            onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(5)
            onNodeWithText("Guest One").performClick()
            assertEquals(302L, personId)
        }

    @Test
    fun testEpisodeDetailScreen_backClicked_invokesOnBackClicked() =
        runComposeUiTest {
            val fakeRepository =
                FakeTvRepository().apply {
                    getEpisodeDetailsResult = Result.success(successfulEpisodeDetail())
                }
            val viewModel = EpisodeDetailScreenModel(tvId = 1, seasonNumber = 1, episodeNumber = 1, tvRepository = fakeRepository)
            var backClicked = false

            setContent {
                EpisodeDetailScreen(
                    tvShowId = 1,
                    seasonNumber = 1,
                    episodeNumber = 1,
                    windowSize = WindowSize.COMPACT,
                    onBackClicked = { backClicked = true },
                    viewModel = viewModel,
                )
            }

            onNodeWithContentDescription("Back").performClick()
            assertEquals(true, backClicked)
        }
}
