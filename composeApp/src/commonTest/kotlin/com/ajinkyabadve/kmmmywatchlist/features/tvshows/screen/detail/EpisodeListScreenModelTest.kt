package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import com.ajinkyabadve.kmmmywatchlist.network.HttpExceptionsTestFactory
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeListScreenModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeTvRepository()

    private lateinit var notFoundException: HttpExceptions

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runTest {
            notFoundException = HttpExceptionsTestFactory.create(HttpStatusCode.NotFound)
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSuccessReturnsSeasonWithEpisodes() = runTest(testDispatcher) {
        val episodes = listOf(Episode(episodeNumber = 1, name = "Pilot"), Episode(episodeNumber = 2, name = "Episode 2"))
        val season = TvSeasonDetail(seasonNumber = 1, name = "Season 1", episodes = episodes)
        fakeRepository.getSeasonDetailsResult = Result.success(season)

        val viewModel = EpisodeListScreenModel(1, 1, fakeRepository)

        val state = assertIs<EpisodeListState.Success>(viewModel.uiState.value)
        assertEquals(episodes, state.season.episodes)
        assertEquals(listOf(1L to 1), fakeRepository.getSeasonDetailsCalls)
    }

    @Test
    fun testHttpExceptionsSetsErrorWithResponseMessage() = runTest(testDispatcher) {
        fakeRepository.getSeasonDetailsResult = Result.failure(notFoundException)

        val viewModel = EpisodeListScreenModel(1, 1, fakeRepository)

        val state = assertIs<EpisodeListState.Error>(viewModel.uiState.value)
        assertEquals(notFoundException.message, state.message)
    }

    @Test
    fun testIOExceptionSetsNetworkErrorMessage() = runTest(testDispatcher) {
        fakeRepository.getSeasonDetailsResult = Result.failure(IOException("Mock network failure"))

        val viewModel = EpisodeListScreenModel(1, 1, fakeRepository)

        val state = assertIs<EpisodeListState.Error>(viewModel.uiState.value)
        assertEquals("Network Connection Error. Please check your internet connectivity.", state.message)
    }

    @Test
    fun testUnexpectedExceptionSetsGenericErrorMessage() = runTest(testDispatcher) {
        fakeRepository.getSeasonDetailsResult = Result.failure(RuntimeException("Boom"))

        val viewModel = EpisodeListScreenModel(1, 1, fakeRepository)

        val state = assertIs<EpisodeListState.Error>(viewModel.uiState.value)
        assertEquals("An unexpected error occurred while loading episodes. Please try again.", state.message)
    }

    @Test
    fun testRetryAfterErrorSucceeds() = runTest(testDispatcher) {
        fakeRepository.getSeasonDetailsResult = Result.failure(IOException("Mock network failure"))
        val viewModel = EpisodeListScreenModel(1, 1, fakeRepository)
        assertIs<EpisodeListState.Error>(viewModel.uiState.value)

        val season = TvSeasonDetail(seasonNumber = 1, name = "Season 1", episodes = listOf(Episode(episodeNumber = 1)))
        fakeRepository.getSeasonDetailsResult = Result.success(season)
        viewModel.loadEpisodes()

        val state = assertIs<EpisodeListState.Success>(viewModel.uiState.value)
        assertEquals(season, state.season)
    }
}
