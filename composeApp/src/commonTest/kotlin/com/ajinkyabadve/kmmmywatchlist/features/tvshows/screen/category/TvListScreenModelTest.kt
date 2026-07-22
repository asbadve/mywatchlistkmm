package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category

import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TvListScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeTvRepository()

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
    fun testInitialLoadPopulatesListAndAllowsPagination() =
        runTest(testDispatcher) {
            fakeRepository.getTvShowsResult =
                Result.success(
                    TvPageResult(page = 1, list = listOf(Tv(id = 1, title = "Tv A")), totalResults = 1, totalPages = 2),
                )

            val viewModel = TvListScreenModel(FETCH_TYPE, fakeRepository)

            assertEquals(ListState.IDLE, viewModel.listState)
            assertEquals(listOf(Tv(id = 1, title = "Tv A")), viewModel.tvList)
            assertEquals(listOf(1 to FETCH_TYPE), fakeRepository.getTvShowsCalls)
        }

    @Test
    fun testEmptyListResultSetsPaginationExhaust() =
        runTest(testDispatcher) {
            fakeRepository.getTvShowsResult =
                Result.success(
                    TvPageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0),
                )

            val viewModel = TvListScreenModel(FETCH_TYPE, fakeRepository)

            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
            assertTrue(viewModel.tvList.isEmpty())
        }

    @Test
    fun testNullTotalPagesIsTreatedAsPaginationExhaust() =
        runTest(testDispatcher) {
            fakeRepository.getTvShowsResult =
                Result.success(
                    TvPageResult(page = 1, list = listOf(Tv(id = 1, title = "Tv A")), totalResults = 1, totalPages = null),
                )

            val viewModel = TvListScreenModel(FETCH_TYPE, fakeRepository)

            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
        }

    @Test
    fun testLoadTvShowsAppendsNextPageWithoutClearingPreviousResults() =
        runTest(testDispatcher) {
            fakeRepository.getTvShowsResult =
                Result.success(
                    TvPageResult(page = 1, list = listOf(Tv(id = 1, title = "Tv A")), totalResults = 2, totalPages = 2),
                )
            val viewModel = TvListScreenModel(FETCH_TYPE, fakeRepository)
            assertEquals(ListState.IDLE, viewModel.listState)

            fakeRepository.getTvShowsResult =
                Result.success(
                    TvPageResult(page = 2, list = listOf(Tv(id = 2, title = "Tv B")), totalResults = 2, totalPages = 2),
                )
            viewModel.loadTvShows()

            assertEquals(listOf(Tv(id = 1, title = "Tv A"), Tv(id = 2, title = "Tv B")), viewModel.tvList)
            assertEquals(listOf(1 to FETCH_TYPE, 2 to FETCH_TYPE), fakeRepository.getTvShowsCalls)
        }

    @Test
    fun testIOExceptionSetsNetworkErrorState() =
        runTest(testDispatcher) {
            fakeRepository.getTvShowsResult = Result.failure(IOException("Mock network failure"))

            val viewModel = TvListScreenModel(FETCH_TYPE, fakeRepository)

            assertEquals(ListState.NETWORK_ERROR, viewModel.listState)
            assertTrue(viewModel.tvList.isEmpty())
        }

    @Test
    fun testUnexpectedExceptionSetsErrorState() =
        runTest(testDispatcher) {
            fakeRepository.getTvShowsResult = Result.failure(SerializationException("Boom"))

            val viewModel = TvListScreenModel(FETCH_TYPE, fakeRepository)

            assertEquals(ListState.ERROR, viewModel.listState)
        }

    @Test
    fun testHttpExceptionsBadRequestSetsNetworkErrorState() =
        runTest(testDispatcher) {
            fakeRepository.getTvShowsResult = Result.failure(badRequestException)

            val viewModel = TvListScreenModel(FETCH_TYPE, fakeRepository)

            assertEquals(ListState.NETWORK_ERROR, viewModel.listState)
        }

    @Test
    fun testHttpExceptionsNotFoundSetsErrorState() =
        runTest(testDispatcher) {
            fakeRepository.getTvShowsResult = Result.failure(notFoundException)

            val viewModel = TvListScreenModel(FETCH_TYPE, fakeRepository)

            assertEquals(ListState.ERROR, viewModel.listState)
        }

    private companion object {
        const val FETCH_TYPE = "popular"
    }
}
