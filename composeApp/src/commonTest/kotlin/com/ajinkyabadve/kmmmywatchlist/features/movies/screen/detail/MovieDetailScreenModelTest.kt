package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.account.model.AccountStates
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeAccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeMovieDetailCacheRepository
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
import kotlin.test.assertTrue

/**
 * The exception-type-to-[UiText] mapping (HttpExceptions/IOException/ContentConvertException/
 * SerializationException) now lives in [com.ajinkyabadve.kmmmywatchlist.core.data.NetworkBoundResource]
 * and is covered against a real repository in `MovieDetailCacheRepositoryImplTest` instead - this
 * class only verifies the ScreenModel's own reaction to whatever
 * [com.ajinkyabadve.kmmmywatchlist.core.data.Resource] the repository hands it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeCacheRepository = FakeMovieDetailCacheRepository()

    private fun buildModel(movieId: Long = 42) = MovieDetailScreenModel(movieId = movieId, movieDetailCacheRepository = fakeCacheRepository)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSuccessLoadsMovieDetail() =
        runTest(testDispatcher) {
            val detail = MovieDetail(id = 42, title = "Fixture Movie")
            fakeCacheRepository.getMovieDetailResult = Result.success(detail)

            val viewModel = buildModel()

            val state = assertIs<MovieDetailState.Success>(viewModel.uiState.value)
            assertEquals(detail, state.movieDetail)
            assertEquals(listOf(42L), fakeCacheRepository.getMovieDetailCalls)
        }

    @Test
    fun testErrorWithNoCachedDataSetsErrorState() =
        runTest(testDispatcher) {
            val failure = IOException("Mock network failure")
            fakeCacheRepository.getMovieDetailResult = Result.failure(failure)

            val viewModel = buildModel()

            val state = assertIs<MovieDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Plain(failure.message.orEmpty()), state.message)
        }

    @Test
    fun testRetryAfterErrorSucceeds() =
        runTest(testDispatcher) {
            fakeCacheRepository.getMovieDetailResult = Result.failure(IOException("Mock network failure"))
            val viewModel = buildModel()
            assertIs<MovieDetailState.Error>(viewModel.uiState.value)

            val detail = MovieDetail(id = 42, title = "Fixture Movie")
            fakeCacheRepository.getMovieDetailResult = Result.success(detail)
            viewModel.loadMovieDetails()

            val state = assertIs<MovieDetailState.Success>(viewModel.uiState.value)
            assertEquals(detail, state.movieDetail)
            assertTrue(fakeCacheRepository.getMovieDetailCalls.size == 2)
        }

    @Test
    fun testLocalCacheSurvivesANetworkErrorOnTheInitialLoad() =
        runTest(testDispatcher) {
            val cachedDetail = MovieDetail(id = 42, title = "Cached Movie")
            fakeCacheRepository.seedCached(42, cachedDetail)
            fakeCacheRepository.getMovieDetailResult = Result.failure(IOException("Mock network failure"))

            val viewModel = buildModel()

            val state = assertIs<MovieDetailState.Success>(viewModel.uiState.value)
            assertEquals(cachedDetail, state.movieDetail)
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
            fakeCacheRepository.getMovieDetailResult = Result.success(MovieDetail(id = 42, title = "Fixture Movie"))

            val viewModel =
                MovieDetailScreenModel(
                    movieId = 42,
                    movieDetailCacheRepository = fakeCacheRepository,
                    authRepository = fakeAuthRepository,
                    accountMediaRepository = fakeAccountMediaRepository,
                    trackedMediaRepository = FakeTrackedMediaRepository(),
                )

            assertTrue(viewModel.mediaActionsState.uiState.value.isFavorite)
            assertTrue(viewModel.mediaActionsState.uiState.value.isInWatchlist)
        }
}
