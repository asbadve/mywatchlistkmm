package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.SeasonSummary
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
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
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_seasons
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AllSeasonsScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeTvRepository()

    // Built in a standalone runTest{}, isolated from each test's own runTest(testDispatcher){} -
    // see MovieListScreenModelTest for why resolving these inline inside a test body breaks
    // UnconfinedTestDispatcher's synchronous execution of the ViewModel's own launch{}.
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
    fun testSuccessReturnsSeasons() =
        runTest(testDispatcher) {
            val seasons = listOf(SeasonSummary(seasonNumber = 1), SeasonSummary(seasonNumber = 2))
            fakeRepository.getTvDetailsResult = Result.success(TvDetail(id = 1, title = "Show", seasons = seasons))

            val viewModel = AllSeasonsScreenModel(1, fakeRepository)

            val state = assertIs<AllSeasonsState.Success>(viewModel.uiState.value)
            assertEquals(seasons, state.seasons)
        }

    @Test
    fun testNullSeasonsResolvesToEmptyList() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult = Result.success(TvDetail(id = 1, title = "Show", seasons = null))

            val viewModel = AllSeasonsScreenModel(1, fakeRepository)

            val state = assertIs<AllSeasonsState.Success>(viewModel.uiState.value)
            assertTrue(state.seasons.isEmpty())
        }

    @Test
    fun testEmptySeasonsList() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult = Result.success(TvDetail(id = 1, title = "Show", seasons = emptyList()))

            val viewModel = AllSeasonsScreenModel(1, fakeRepository)

            val state = assertIs<AllSeasonsState.Success>(viewModel.uiState.value)
            assertTrue(state.seasons.isEmpty())
        }

    @Test
    fun testHttpExceptionsSetsErrorWithResponseMessage() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult = Result.failure(notFoundException)

            val viewModel = AllSeasonsScreenModel(1, fakeRepository)

            val state = assertIs<AllSeasonsState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Plain(notFoundException.message), state.message)
        }

    @Test
    fun testIOExceptionSetsNetworkErrorMessage() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult = Result.failure(IOException("Mock network failure"))

            val viewModel = AllSeasonsScreenModel(1, fakeRepository)

            val state = assertIs<AllSeasonsState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_network), state.message)
        }

    @Test
    fun testSerializationExceptionSetsGenericErrorMessage() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult = Result.failure(SerializationException("Boom"))

            val viewModel = AllSeasonsScreenModel(1, fakeRepository)

            val state = assertIs<AllSeasonsState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_unexpected_seasons), state.message)
        }

    @Test
    fun testRetryAfterErrorSucceeds() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult = Result.failure(IOException("Mock network failure"))
            val viewModel = AllSeasonsScreenModel(1, fakeRepository)
            assertIs<AllSeasonsState.Error>(viewModel.uiState.value)

            val seasons = listOf(SeasonSummary(seasonNumber = 1))
            fakeRepository.getTvDetailsResult = Result.success(TvDetail(id = 1, title = "Show", seasons = seasons))
            viewModel.loadSeasons()

            val state = assertIs<AllSeasonsState.Success>(viewModel.uiState.value)
            assertEquals(seasons, state.seasons)
        }
}
