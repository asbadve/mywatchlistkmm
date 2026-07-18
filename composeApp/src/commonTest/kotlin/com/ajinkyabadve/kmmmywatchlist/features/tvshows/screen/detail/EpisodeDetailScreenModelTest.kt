package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.BackdropImage
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.EpisodeDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.EpisodeImagesResponse
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
class EpisodeDetailScreenModelTest {

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
    fun testSuccessReturnsEpisodeWithImages() = runTest(testDispatcher) {
        val episode = EpisodeDetail(
            episodeNumber = 1,
            seasonNumber = 1,
            name = "Pilot",
            images = EpisodeImagesResponse(stills = listOf(BackdropImage(filePath = "/still.jpg"))),
        )
        fakeRepository.getEpisodeDetailsResult = Result.success(episode)

        val viewModel = EpisodeDetailScreenModel(1, 1, 1, fakeRepository)

        val state = assertIs<EpisodeDetailState.Success>(viewModel.uiState.value)
        assertEquals(episode, state.episode)
        assertEquals(listOf(Triple(1L, 1, 1)), fakeRepository.getEpisodeDetailsCalls)
    }

    @Test
    fun testHttpExceptionsSetsErrorWithResponseMessage() = runTest(testDispatcher) {
        fakeRepository.getEpisodeDetailsResult = Result.failure(notFoundException)

        val viewModel = EpisodeDetailScreenModel(1, 1, 1, fakeRepository)

        val state = assertIs<EpisodeDetailState.Error>(viewModel.uiState.value)
        assertEquals(notFoundException.message, state.message)
    }

    @Test
    fun testIOExceptionSetsNetworkErrorMessage() = runTest(testDispatcher) {
        fakeRepository.getEpisodeDetailsResult = Result.failure(IOException("Mock network failure"))

        val viewModel = EpisodeDetailScreenModel(1, 1, 1, fakeRepository)

        val state = assertIs<EpisodeDetailState.Error>(viewModel.uiState.value)
        assertEquals("Network Connection Error. Please check your internet connectivity.", state.message)
    }

    @Test
    fun testUnexpectedExceptionSetsGenericErrorMessage() = runTest(testDispatcher) {
        fakeRepository.getEpisodeDetailsResult = Result.failure(RuntimeException("Boom"))

        val viewModel = EpisodeDetailScreenModel(1, 1, 1, fakeRepository)

        val state = assertIs<EpisodeDetailState.Error>(viewModel.uiState.value)
        assertEquals("An unexpected error occurred while loading the episode. Please try again.", state.message)
    }

    @Test
    fun testRetryAfterErrorSucceeds() = runTest(testDispatcher) {
        fakeRepository.getEpisodeDetailsResult = Result.failure(IOException("Mock network failure"))
        val viewModel = EpisodeDetailScreenModel(1, 1, 1, fakeRepository)
        assertIs<EpisodeDetailState.Error>(viewModel.uiState.value)

        val episode = EpisodeDetail(episodeNumber = 1, seasonNumber = 1, name = "Pilot")
        fakeRepository.getEpisodeDetailsResult = Result.success(episode)
        viewModel.loadEpisodeDetails()

        val state = assertIs<EpisodeDetailState.Success>(viewModel.uiState.value)
        assertEquals(episode, state.episode)
    }
}
