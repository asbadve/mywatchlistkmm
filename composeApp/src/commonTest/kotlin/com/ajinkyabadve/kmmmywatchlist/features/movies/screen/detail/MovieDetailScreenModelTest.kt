package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.account.model.AccountStates
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeAccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeMovieDetailCacheRepository
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
import mywatchlist.composeapp.generated.resources.error_unexpected_movie_details
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeCacheRepository = FakeMovieDetailCacheRepository()

    private fun buildModel(movieId: Long = 42) = MovieDetailScreenModel(movieId = movieId, movieDetailCacheRepository = fakeCacheRepository)

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
    fun testSuccessLoadsMovieDetail() =
        runTest(testDispatcher) {
            val detail = MovieDetail(id = 42, title = "Fixture Movie")
            fakeCacheRepository.refreshResult = Result.success(detail)

            val viewModel = buildModel()

            val state = assertIs<MovieDetailState.Success>(viewModel.uiState.value)
            assertEquals(detail, state.movieDetail)
            assertEquals(listOf(42L), fakeCacheRepository.refreshCalls)
        }

    @Test
    fun testHttpExceptionsSetsErrorWithResponseMessage() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult = Result.failure(notFoundException)

            val viewModel = buildModel()

            val state = assertIs<MovieDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Plain(notFoundException.message), state.message)
        }

    @Test
    fun testIOExceptionSetsNetworkErrorMessage() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult = Result.failure(IOException("Mock network failure"))

            val viewModel = buildModel()

            val state = assertIs<MovieDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_network), state.message)
        }

    @Test
    fun testSerializationExceptionSetsGenericErrorMessage() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult = Result.failure(SerializationException("Boom"))

            val viewModel = buildModel()

            val state = assertIs<MovieDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_unexpected_movie_details), state.message)
        }

    @Test
    fun testRetryAfterErrorSucceeds() =
        runTest(testDispatcher) {
            fakeCacheRepository.refreshResult = Result.failure(IOException("Mock network failure"))
            val viewModel = buildModel()
            assertIs<MovieDetailState.Error>(viewModel.uiState.value)

            val detail = MovieDetail(id = 42, title = "Fixture Movie")
            fakeCacheRepository.refreshResult = Result.success(detail)
            viewModel.loadMovieDetails()

            val state = assertIs<MovieDetailState.Success>(viewModel.uiState.value)
            assertEquals(detail, state.movieDetail)
            assertTrue(fakeCacheRepository.refreshCalls.size == 2)
        }

    @Test
    fun testLocalCacheSurvivesANetworkErrorOnTheInitialLoad() =
        runTest(testDispatcher) {
            val cachedDetail = MovieDetail(id = 42, title = "Cached Movie")
            fakeCacheRepository.seedCached(42, cachedDetail)
            fakeCacheRepository.refreshResult = Result.failure(IOException("Mock network failure"))

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
            fakeCacheRepository.refreshResult = Result.success(MovieDetail(id = 42, title = "Fixture Movie"))

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
