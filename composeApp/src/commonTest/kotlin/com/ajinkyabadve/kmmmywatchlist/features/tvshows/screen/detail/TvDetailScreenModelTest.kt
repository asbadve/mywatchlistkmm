package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.account.model.AccountStates
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeAccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.SeasonSummary
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.FakeTvDetailCacheRepository
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
    private val fakeCacheRepository = FakeTvDetailCacheRepository()

    private fun buildModel(tvId: Long = 1) = TvDetailScreenModel(tvId = tvId, tvDetailCacheRepository = fakeCacheRepository)

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
            fakeCacheRepository.refreshResult =
                Result.success(
                    TvDetail(id = 1, title = "Show", seasons = listOf(SeasonSummary(seasonNumber = 1), SeasonSummary(seasonNumber = 2))),
                )
            fakeCacheRepository.refreshSeasonsResult =
                mapOf(
                    1 to
                        TvSeasonDetail(
                            seasonNumber = 1,
                            episodes = listOf(Episode(seasonNumber = 1, episodeNumber = 1, airDate = "2000-01-01")),
                        ),
                    2 to
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

            val viewModel = buildModel()

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(2, state.currentSeason?.seasonNumber)
            assertEquals(1, state.latestReleasedEpisodeNumber)
            assertEquals(setOf(1, 2), state.allSeasonDetails.keys)
        }

    @Test
    fun testAnUnreleasedFutureSeasonIsIgnoredInFavorOfTheLatestReleasedOne() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult =
                Result.success(
                    TvDetail(id = 1, title = "Show", seasons = listOf(SeasonSummary(seasonNumber = 1), SeasonSummary(seasonNumber = 2))),
                )
            fakeCacheRepository.refreshSeasonsResult =
                mapOf(
                    1 to
                        TvSeasonDetail(
                            seasonNumber = 1,
                            episodes = listOf(Episode(seasonNumber = 1, episodeNumber = 5, airDate = "2000-01-01")),
                        ),
                    2 to
                        TvSeasonDetail(
                            seasonNumber = 2,
                            episodes = listOf(Episode(seasonNumber = 2, episodeNumber = 1, airDate = "2099-01-01")),
                        ),
                )

            val viewModel = buildModel()

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(1, state.currentSeason?.seasonNumber)
            assertEquals(5, state.latestReleasedEpisodeNumber)
        }

    @Test
    fun testFallsBackToEarliestSeasonWhenNothingHasReleasedYet() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult =
                Result.success(
                    TvDetail(
                        id = 1,
                        title = "Show",
                        seasons = listOf(SeasonSummary(seasonNumber = 0), SeasonSummary(seasonNumber = 1), SeasonSummary(seasonNumber = 2)),
                    ),
                )
            fakeCacheRepository.refreshSeasonsResult =
                listOf(0, 1, 2).associateWith { seasonNumber ->
                    TvSeasonDetail(
                        seasonNumber = seasonNumber,
                        episodes = listOf(Episode(seasonNumber = seasonNumber, episodeNumber = 1, airDate = "2099-01-01")),
                    )
                }

            val viewModel = buildModel()

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(1, state.currentSeason?.seasonNumber)
            assertNull(state.latestReleasedEpisodeNumber)
        }

    @Test
    fun testEmptySeasonsListResultsInEmptyAllSeasonDetailsAndNullCurrentSeason() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult = Result.success(TvDetail(id = 1, title = "Show", seasons = emptyList()))

            val viewModel = buildModel()

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertTrue(state.allSeasonDetails.isEmpty())
            assertNull(state.currentSeason)
        }

    /** A season the repository couldn't fetch/cache is simply absent from the map it hands back -
     *  the ViewModel just renders whatever it's given; the fetch-fallback behavior itself is
     *  covered at the repository layer in `TvDetailCacheRepositoryImplTest`. */
    @Test
    fun testASeasonMissingFromTheRepositoryResultIsExcludedFromTheMap() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult =
                Result.success(
                    TvDetail(id = 1, title = "Show", seasons = listOf(SeasonSummary(seasonNumber = 1), SeasonSummary(seasonNumber = 2))),
                )
            fakeCacheRepository.refreshSeasonsResult = mapOf(1 to TvSeasonDetail(seasonNumber = 1))

            val viewModel = buildModel()

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(setOf(1), state.allSeasonDetails.keys)
            assertEquals(1, state.currentSeason?.seasonNumber)
        }

    @Test
    fun testHttpExceptionsSetsErrorWithResponseMessage() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult = Result.failure(notFoundException)

            val viewModel = buildModel()

            val state = assertIs<TvDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Plain(notFoundException.message), state.message)
        }

    @Test
    fun testIOExceptionSetsNetworkErrorMessage() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult = Result.failure(IOException("Mock network failure"))

            val viewModel = buildModel()

            val state = assertIs<TvDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_network), state.message)
        }

    @Test
    fun testSerializationExceptionSetsGenericErrorMessage() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult = Result.failure(SerializationException("Boom"))

            val viewModel = buildModel()

            val state = assertIs<TvDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_unexpected_tv_details), state.message)
        }

    @Test
    fun testRetryAfterErrorSucceeds() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult = Result.failure(IOException("Mock network failure"))
            val viewModel = buildModel()
            assertIs<TvDetailState.Error>(viewModel.uiState.value)

            fakeCacheRepository.refreshResult =
                Result.success(TvDetail(id = 1, title = "Show", seasons = listOf(SeasonSummary(seasonNumber = 1))))
            viewModel.loadTvDetails()

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals(1, state.tvDetail.id)
        }

    @Test
    fun testLocalCacheSurvivesANetworkErrorOnTheInitialLoad() =
        runTest(testDispatcher) {
            val cachedDetail = TvDetail(id = 1, title = "Cached Show", seasons = listOf(SeasonSummary(seasonNumber = 1)))
            fakeCacheRepository.seedCached(1, cachedDetail)
            fakeCacheRepository.seedCachedSeason(1, TvSeasonDetail(seasonNumber = 1, name = "Cached Season"))
            fakeCacheRepository.refreshResult = Result.failure(IOException("Mock network failure"))

            val viewModel = buildModel()

            val state = assertIs<TvDetailState.Success>(viewModel.uiState.value)
            assertEquals("Cached Show", state.tvDetail.title)
            assertEquals("Cached Season", state.currentSeason?.name)
        }

    @Test
    fun testSuccessfulLoadTriggersExactlyOneRefresh() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult =
                Result.success(TvDetail(id = 1, title = "Show", seasons = listOf(SeasonSummary(seasonNumber = 1))))

            buildModel()

            assertEquals(listOf(1L), fakeCacheRepository.refreshCalls)
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
            fakeCacheRepository.refreshResult = Result.success(TvDetail(id = 1, title = "Show"))

            val viewModel =
                TvDetailScreenModel(
                    tvId = 1,
                    tvDetailCacheRepository = fakeCacheRepository,
                    authRepository = fakeAuthRepository,
                    accountMediaRepository = fakeAccountMediaRepository,
                    trackedMediaRepository = FakeTrackedMediaRepository(),
                )

            assertTrue(viewModel.mediaActionsState.uiState.value.isFavorite)
            assertTrue(viewModel.mediaActionsState.uiState.value.isInWatchlist)
        }
}
