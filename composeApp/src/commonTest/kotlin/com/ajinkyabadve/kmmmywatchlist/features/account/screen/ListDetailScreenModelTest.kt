package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeCustomListRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
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
import kotlin.test.assertTrue

private object ListDetailScreenModelTestConstant {
    const val LIST_ID = 5861L
    const val SESSION_ID = "session_abc"
}

@OptIn(ExperimentalCoroutinesApi::class)
class ListDetailScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeListsRepository = FakeListsRepository()
    private val fakeCustomListRepository = FakeCustomListRepository()

    // Built in a standalone runTest{}, isolated from each test's own runTest(testDispatcher){} -
    // see MovieListScreenModelTest's identical setup for why resolving this inline inside a test
    // body confuses UnconfinedTestDispatcher's eager execution of the ViewModel's own launch{}.
    private lateinit var forbiddenException: HttpExceptions

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runTest {
            forbiddenException = HttpExceptionsTestFactory.create(HttpStatusCode.Forbidden)
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildModel() =
        ListDetailScreenModel(
            listId = ListDetailScreenModelTestConstant.LIST_ID,
            sessionId = ListDetailScreenModelTestConstant.SESSION_ID,
            listsRepository = fakeListsRepository,
            customListRepository = fakeCustomListRepository,
        )

    @Test
    fun testInitialLoadPopulatesItems() =
        runTest(testDispatcher) {
            fakeCustomListRepository.refreshListDetailResult =
                Result.success(TmdbListDetail(name = "My List", items = listOf(Movie(id = 1, title = "Movie A"))))

            val viewModel = buildModel()

            val state = viewModel.uiState
            assertTrue(state is ListDetailState.Success)
            assertEquals("My List", state.detail.name)
            assertEquals(1, state.detail.items.size)
        }

    @Test
    fun testRemoveMovieRemovesItemFromState() =
        runTest(testDispatcher) {
            fakeCustomListRepository.refreshListDetailResult =
                Result.success(
                    TmdbListDetail(
                        name = "My List",
                        items = listOf(Movie(id = 1, title = "Movie A"), Movie(id = 2, title = "Movie B")),
                    ),
                )
            val viewModel = buildModel()

            viewModel.removeMovie(1L)

            val state = viewModel.uiState
            assertTrue(state is ListDetailState.Success)
            assertEquals(1, state.detail.items.size)
            assertEquals(
                "Movie B",
                state.detail.items
                    .first()
                    .title,
            )
            assertEquals(listOf(1L), fakeListsRepository.removeMovieFromListCalls)
            assertEquals(listOf(ListDetailScreenModelTestConstant.LIST_ID to 1L), fakeCustomListRepository.confirmItemDeleteCalls)
        }

    /** Removing while offline queues the delete locally instead of putting the item back. */
    @Test
    fun testRemoveMovieWhileOfflineQueuesTheLocalDeleteAndKeepsTheItemHidden() =
        runTest(testDispatcher) {
            fakeCustomListRepository.refreshListDetailResult =
                Result.success(TmdbListDetail(name = "My List", items = listOf(Movie(id = 1, title = "Movie A"))))
            fakeListsRepository.removeMovieFromListResult = Result.failure(IOException("Mock network failure"))
            val viewModel = buildModel()

            viewModel.removeMovie(1L)

            val state = viewModel.uiState
            assertTrue(state is ListDetailState.Success)
            assertTrue(state.detail.items.isEmpty())
            assertEquals(listOf(ListDetailScreenModelTestConstant.LIST_ID to 1L), fakeCustomListRepository.markItemPendingDeleteCalls)
            assertTrue(fakeCustomListRepository.confirmItemDeleteCalls.isEmpty())
            assertTrue(fakeCustomListRepository.clearItemPendingDeleteCalls.isEmpty())
        }

    /** A real server-side rejection (not offline) puts the item back and clears the queued delete. */
    @Test
    fun testRemoveMovieFailingOutrightRestoresTheItemAndClearsTheQueuedDelete() =
        runTest(testDispatcher) {
            fakeCustomListRepository.refreshListDetailResult =
                Result.success(TmdbListDetail(name = "My List", items = listOf(Movie(id = 1, title = "Movie A"))))
            fakeListsRepository.removeMovieFromListResult = Result.failure(forbiddenException)
            val viewModel = buildModel()

            viewModel.removeMovie(1L)

            val state = viewModel.uiState
            assertTrue(state is ListDetailState.Success)
            assertEquals(listOf("Movie A"), state.detail.items.map { it.title })
            assertEquals(listOf(ListDetailScreenModelTestConstant.LIST_ID to 1L), fakeCustomListRepository.clearItemPendingDeleteCalls)
        }

    @Test
    fun testDeleteListSetsIsDeleted() =
        runTest(testDispatcher) {
            fakeCustomListRepository.refreshListDetailResult = Result.success(TmdbListDetail(name = "My List"))
            val viewModel = buildModel()

            viewModel.deleteList()

            assertTrue(viewModel.isDeleted)
            assertTrue(fakeListsRepository.deleteListCalled)
            assertEquals(listOf(ListDetailScreenModelTestConstant.LIST_ID), fakeCustomListRepository.removeListLocallyCalls)
        }

    @Test
    fun testNetworkErrorSetsErrorState() =
        runTest(testDispatcher) {
            fakeCustomListRepository.refreshListDetailResult = Result.failure(IOException("Mock network failure"))

            val viewModel = buildModel()

            assertTrue(viewModel.uiState is ListDetailState.Error)
        }

    @Test
    fun testLocalCacheSurvivesANetworkErrorOnTheInitialLoad() =
        runTest(testDispatcher) {
            fakeCustomListRepository.seedListDetail(
                ListDetailScreenModelTestConstant.LIST_ID,
                TmdbListDetail(name = "Cached List", items = listOf(Movie(id = 1, title = "Cached Movie"))),
            )
            fakeCustomListRepository.refreshListDetailResult = Result.failure(IOException("Mock network failure"))

            val viewModel = buildModel()

            val state = viewModel.uiState
            assertTrue(state is ListDetailState.Success)
            assertEquals("Cached List", state.detail.name)
            assertEquals(listOf("Cached Movie"), state.detail.items.map { it.title })
        }
}
