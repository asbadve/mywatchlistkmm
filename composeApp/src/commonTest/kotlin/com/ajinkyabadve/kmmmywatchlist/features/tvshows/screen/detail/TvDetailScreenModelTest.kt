package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
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
import mywatchlist.composeapp.generated.resources.error_unexpected_tv_details
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TvDetailScreenModelTest {
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
    fun testNextEpisodeToAirResolvesCurrentSeason() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = 1,
                        title = "Show",
                        seasons = listOf(SeasonSummary(seasonNumber = 1), SeasonSummary(seasonNumber = 2)),
                        nextEpisodeToAir = Episode(seasonNumber = 2),
                        lastEpisodeToAir = Episode(seasonNumber = 1),
                    ),
                )

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(2, state.currentSeason?.seasonNumber)
            assertEquals(setOf(1, 2), state.allSeasonDetails.keys)
        }

    @Test
    fun testLastEpisodeToAirResolvesCurrentSeasonWhenNoNextEpisode() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = 1,
                        title = "Show",
                        seasons = listOf(SeasonSummary(seasonNumber = 1), SeasonSummary(seasonNumber = 2)),
                        nextEpisodeToAir = null,
                        lastEpisodeToAir = Episode(seasonNumber = 1),
                    ),
                )

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(1, state.currentSeason?.seasonNumber)
        }

    @Test
    fun testFallsBackToMaxSeasonNumberWhenNoEpisodesToAir() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = 1,
                        title = "Show",
                        seasons =
                            listOf(
                                SeasonSummary(seasonNumber = 0),
                                SeasonSummary(seasonNumber = 1),
                                SeasonSummary(seasonNumber = 2),
                            ),
                        nextEpisodeToAir = null,
                        lastEpisodeToAir = null,
                    ),
                )

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(2, state.currentSeason?.seasonNumber)
        }

    @Test
    fun testEmptySeasonsListResultsInEmptyAllSeasonDetailsAndNullCurrentSeason() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(id = 1, title = "Show", seasons = emptyList()),
                )

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertTrue(state.allSeasonDetails.isEmpty())
            assertNull(state.currentSeason)
        }

    @Test
    fun testPartialSeasonFetchFailureExcludesFailedSeasonFromMap() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = 1,
                        title = "Show",
                        seasons = listOf(SeasonSummary(seasonNumber = 1), SeasonSummary(seasonNumber = 2)),
                        nextEpisodeToAir = null,
                        lastEpisodeToAir = null,
                    ),
                )
            fakeRepository.getSeasonDetailsResultsByNumber[2] = Result.failure(IOException("season 2 unavailable"))

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(setOf(1), state.allSeasonDetails.keys)
            // Season 2 would have been picked as current (max seasonNumber) but its fetch failed, so it's absent.
            assertNull(state.currentSeason)
        }

    @Test
    fun testHttpExceptionsSetsErrorWithResponseMessage() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult = Result.failure(notFoundException)

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Plain(notFoundException.message), state.message)
        }

    @Test
    fun testIOExceptionSetsNetworkErrorMessage() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult = Result.failure(IOException("Mock network failure"))

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_network), state.message)
        }

    @Test
    fun testSerializationExceptionSetsGenericErrorMessage() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult = Result.failure(SerializationException("Boom"))

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_unexpected_tv_details), state.message)
        }

    @Test
    fun testRetryAfterErrorSucceeds() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult = Result.failure(IOException("Mock network failure"))
            val viewModel = TvDetailScreenModel(1, fakeRepository)
            assertIs<TvDetailState.Error>(viewModel.uiState.value)

            fakeRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(id = 1, title = "Show", seasons = listOf(SeasonSummary(seasonNumber = 1))),
                )
            viewModel.loadTvDetails()

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(1, state.tvDetail.id)
        }
}
