package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.account.model.AccountStates
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeAccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.SeasonSummary
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
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
    fun testLatestReleasedEpisodeAcrossSeasonsResolvesCurrentSeasonAndEpisode() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = 1,
                        title = "Show",
                        seasons = listOf(SeasonSummary(seasonNumber = 1), SeasonSummary(seasonNumber = 2)),
                    ),
                )
            fakeRepository.getSeasonDetailsResultsByNumber[1] =
                Result.success(
                    TvSeasonDetail(
                        seasonNumber = 1,
                        episodes = listOf(Episode(seasonNumber = 1, episodeNumber = 1, airDate = "2000-01-01")),
                    ),
                )
            fakeRepository.getSeasonDetailsResultsByNumber[2] =
                Result.success(
                    TvSeasonDetail(
                        seasonNumber = 2,
                        episodes =
                            listOf(
                                Episode(seasonNumber = 2, episodeNumber = 1, airDate = "2000-02-01"),
                                // Not yet released - a season announced ahead of time must not win.
                                Episode(seasonNumber = 2, episodeNumber = 2, airDate = "2099-01-01"),
                            ),
                    ),
                )

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(2, state.currentSeason?.seasonNumber)
            assertEquals(1, state.latestReleasedEpisodeNumber)
            assertEquals(setOf(1, 2), state.allSeasonDetails.keys)
        }

    @Test
    fun testAnUnreleasedFutureSeasonIsIgnoredInFavorOfTheLatestReleasedOne() =
        runTest(testDispatcher) {
            fakeRepository.getTvDetailsResult =
                Result.success(
                    TvDetail(
                        id = 1,
                        title = "Show",
                        seasons = listOf(SeasonSummary(seasonNumber = 1), SeasonSummary(seasonNumber = 2)),
                    ),
                )
            fakeRepository.getSeasonDetailsResultsByNumber[1] =
                Result.success(
                    TvSeasonDetail(
                        seasonNumber = 1,
                        episodes = listOf(Episode(seasonNumber = 1, episodeNumber = 5, airDate = "2000-01-01")),
                    ),
                )
            fakeRepository.getSeasonDetailsResultsByNumber[2] =
                Result.success(
                    TvSeasonDetail(
                        seasonNumber = 2,
                        episodes = listOf(Episode(seasonNumber = 2, episodeNumber = 1, airDate = "2099-01-01")),
                    ),
                )

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(1, state.currentSeason?.seasonNumber)
            assertEquals(5, state.latestReleasedEpisodeNumber)
        }

    @Test
    fun testFallsBackToEarliestSeasonWhenNothingHasReleasedYet() =
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
                    ),
                )
            listOf(0, 1, 2).forEach { seasonNumber ->
                fakeRepository.getSeasonDetailsResultsByNumber[seasonNumber] =
                    Result.success(
                        TvSeasonDetail(
                            seasonNumber = seasonNumber,
                            episodes = listOf(Episode(seasonNumber = seasonNumber, episodeNumber = 1, airDate = "2099-01-01")),
                        ),
                    )
            }

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(1, state.currentSeason?.seasonNumber)
            assertNull(state.latestReleasedEpisodeNumber)
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
                    ),
                )
            fakeRepository.getSeasonDetailsResultsByNumber[2] = Result.failure(IOException("season 2 unavailable"))

            val viewModel = TvDetailScreenModel(1, fakeRepository)

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(setOf(1), state.allSeasonDetails.keys)
            // Season 2's fetch failed, so only season 1 remains - and with nothing released in it
            // (the default fake season has no episodes), it's still the fallback "current" season.
            assertEquals(1, state.currentSeason?.seasonNumber)
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

    /**
     * The ViewModel itself subscribes to `AuthRepository.sessionState` and drives the
     * `account_states` pre-check the moment a session exists - `MediaActionButtons` never triggers
     * this (see `MediaActionsState`'s kdoc), so this is the only place that behavior is verified.
     */
    @Test
    fun testAlreadyLoggedInSessionTriggersAccountStatesLoadOnConstruction() =
        runTest(testDispatcher) {
            val fakeAuthRepository = FakeAuthRepository()
            fakeAuthRepository.saveSession(UserSession(sessionId = "session_abc", accountId = 100L, username = "jane", name = "Jane"))
            val fakeAccountMediaRepository =
                FakeAccountMediaRepository().apply {
                    accountStatesResult = Result.success(AccountStates(favorite = true, watchlist = true))
                }
            fakeRepository.getTvDetailsResult = Result.success(TvDetail(id = 1, title = "Show"))

            val viewModel =
                TvDetailScreenModel(
                    tvId = 1,
                    tvRepository = fakeRepository,
                    authRepository = fakeAuthRepository,
                    accountMediaRepository = fakeAccountMediaRepository,
                )

            assertTrue(viewModel.mediaActionsState.uiState.value.isFavorite)
            assertTrue(viewModel.mediaActionsState.uiState.value.isInWatchlist)
        }
}
