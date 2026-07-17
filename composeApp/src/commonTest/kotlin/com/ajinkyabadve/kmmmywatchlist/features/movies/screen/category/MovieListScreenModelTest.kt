package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
class MovieListScreenModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeMovieRepository()

    // Built in a standalone runTest{}, isolated from each test's own runTest(testDispatcher){},
    // since resolving these inline inside a test body confuses UnconfinedTestDispatcher's eager
    // execution of the ViewModel's own launch{} (it stops completing synchronously).
    private lateinit var badRequestException: HttpExceptions
    private lateinit var notFoundException: HttpExceptions

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runTest {
            badRequestException = HttpExceptionsTestFactory.create(HttpStatusCode.BadRequest)
            notFoundException = HttpExceptionsTestFactory.create(HttpStatusCode.NotFound)
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadPopulatesListAndAllowsPagination() = runTest(testDispatcher) {
        fakeRepository.getMoviesResult = Result.success(
            MoviePageResult(page = 1, list = listOf(Movie(id = 1, title = "Movie A")), totalResults = 1, totalPages = 2)
        )

        val viewModel = MovieListScreenModel(FETCH_TYPE, fakeRepository)

        assertEquals(ListState.IDLE, viewModel.listState)
        assertEquals(listOf(Movie(id = 1, title = "Movie A")), viewModel.movieList)
        assertEquals(listOf(1 to FETCH_TYPE), fakeRepository.getMoviesCalls)
    }

    @Test
    fun testEmptyListResultSetsPaginationExhaust() = runTest(testDispatcher) {
        fakeRepository.getMoviesResult = Result.success(
            MoviePageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0)
        )

        val viewModel = MovieListScreenModel(FETCH_TYPE, fakeRepository)

        assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
        assertTrue(viewModel.movieList.isEmpty())
    }

    @Test
    fun testLoadMoviesAppendsNextPageWithoutClearingPreviousResults() = runTest(testDispatcher) {
        fakeRepository.getMoviesResult = Result.success(
            MoviePageResult(page = 1, list = listOf(Movie(id = 1, title = "Movie A")), totalResults = 2, totalPages = 2)
        )
        val viewModel = MovieListScreenModel(FETCH_TYPE, fakeRepository)
        assertEquals(ListState.IDLE, viewModel.listState)

        fakeRepository.getMoviesResult = Result.success(
            MoviePageResult(page = 2, list = listOf(Movie(id = 2, title = "Movie B")), totalResults = 2, totalPages = 2)
        )
        viewModel.loadMovies()

        assertEquals(
            listOf(Movie(id = 1, title = "Movie A"), Movie(id = 2, title = "Movie B")),
            viewModel.movieList
        )
        assertEquals(listOf(1 to FETCH_TYPE, 2 to FETCH_TYPE), fakeRepository.getMoviesCalls)
    }

    @Test
    fun testIOExceptionSetsNetworkErrorState() = runTest(testDispatcher) {
        fakeRepository.getMoviesResult = Result.failure(IOException("Mock network failure"))

        val viewModel = MovieListScreenModel(FETCH_TYPE, fakeRepository)

        assertEquals(ListState.NETWORK_ERROR, viewModel.listState)
        assertTrue(viewModel.movieList.isEmpty())
    }

    @Test
    fun testUnexpectedExceptionSetsErrorState() = runTest(testDispatcher) {
        fakeRepository.getMoviesResult = Result.failure(RuntimeException("Boom"))

        val viewModel = MovieListScreenModel(FETCH_TYPE, fakeRepository)

        assertEquals(ListState.ERROR, viewModel.listState)
    }

    @Test
    fun testHttpExceptionsBadRequestSetsNetworkErrorState() = runTest(testDispatcher) {
        fakeRepository.getMoviesResult = Result.failure(badRequestException)

        val viewModel = MovieListScreenModel(FETCH_TYPE, fakeRepository)

        assertEquals(ListState.NETWORK_ERROR, viewModel.listState)
    }

    @Test
    fun testHttpExceptionsNotFoundSetsErrorState() = runTest(testDispatcher) {
        fakeRepository.getMoviesResult = Result.failure(notFoundException)

        val viewModel = MovieListScreenModel(FETCH_TYPE, fakeRepository)

        assertEquals(ListState.ERROR, viewModel.listState)
    }

    private companion object {
        const val FETCH_TYPE = "now_playing"
    }
}
