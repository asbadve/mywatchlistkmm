package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
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
    private val fakeRepository = FakeMovieRepository()

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
            fakeRepository.getMovieDetailsResult = Result.success(detail)

            val viewModel = MovieDetailScreenModel(42, fakeRepository)

            val state = assertIs<MovieDetailState.Success>(viewModel.uiState.value)
            assertEquals(detail, state.movieDetail)
            assertEquals(listOf(42L), fakeRepository.getMovieDetailsCalls)
        }

    @Test
    fun testHttpExceptionsSetsErrorWithResponseMessage() =
        runTest(testDispatcher) {
            fakeRepository.getMovieDetailsResult = Result.failure(notFoundException)

            val viewModel = MovieDetailScreenModel(42, fakeRepository)

            val state = assertIs<MovieDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Plain(notFoundException.message), state.message)
        }

    @Test
    fun testIOExceptionSetsNetworkErrorMessage() =
        runTest(testDispatcher) {
            fakeRepository.getMovieDetailsResult = Result.failure(IOException("Mock network failure"))

            val viewModel = MovieDetailScreenModel(42, fakeRepository)

            val state = assertIs<MovieDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_network), state.message)
        }

    @Test
    fun testSerializationExceptionSetsGenericErrorMessage() =
        runTest(testDispatcher) {
            fakeRepository.getMovieDetailsResult = Result.failure(SerializationException("Boom"))

            val viewModel = MovieDetailScreenModel(42, fakeRepository)

            val state = assertIs<MovieDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_unexpected_movie_details), state.message)
        }

    @Test
    fun testRetryAfterErrorSucceeds() =
        runTest(testDispatcher) {
            fakeRepository.getMovieDetailsResult = Result.failure(IOException("Mock network failure"))
            val viewModel = MovieDetailScreenModel(42, fakeRepository)
            assertIs<MovieDetailState.Error>(viewModel.uiState.value)

            val detail = MovieDetail(id = 42, title = "Fixture Movie")
            fakeRepository.getMovieDetailsResult = Result.success(detail)
            viewModel.loadMovieDetails()

            val state = assertIs<MovieDetailState.Success>(viewModel.uiState.value)
            assertEquals(detail, state.movieDetail)
            assertTrue(fakeRepository.getMovieDetailsCalls.size == 2)
        }
}
