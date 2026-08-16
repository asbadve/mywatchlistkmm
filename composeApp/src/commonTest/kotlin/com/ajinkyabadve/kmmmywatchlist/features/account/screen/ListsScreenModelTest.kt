package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListPageResult
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
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

private object ListsScreenModelTestConstant {
    const val ACCOUNT_ID = 100L
    const val SESSION_ID = "session_abc"
}

@OptIn(ExperimentalCoroutinesApi::class)
class ListsScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeListsRepository = FakeListsRepository()
    private val fakeAuthRepository = FakeAuthRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildModel() =
        ListsScreenModel(
            accountId = ListsScreenModelTestConstant.ACCOUNT_ID,
            sessionId = ListsScreenModelTestConstant.SESSION_ID,
            listsRepository = fakeListsRepository,
            authRepository = fakeAuthRepository,
        )

    @Test
    fun testInitialLoadPopulatesLists() =
        runTest(testDispatcher) {
            fakeListsRepository.listsResult =
                Result.success(
                    TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 1, name = "My List", itemCount = 3)), totalPages = 1),
                )

            val viewModel = buildModel()

            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
            assertEquals(1, viewModel.lists.size)
            assertEquals("My List", viewModel.lists.first().name)
        }

    @Test
    fun testCreateListAddsNewListAndInvokesCallback() =
        runTest(testDispatcher) {
            fakeListsRepository.createListResult = Result.success(555L)
            val viewModel = buildModel()

            var createdListId: Long? = null
            viewModel.createList("New List", "desc") { listId -> createdListId = listId }

            assertEquals(555L, createdListId)
        }

    @Test
    fun testEmptyListsSetsPaginationExhaust() =
        runTest(testDispatcher) {
            fakeListsRepository.listsResult = Result.success(TmdbListPageResult(page = 1, list = emptyList(), totalPages = 0))

            val viewModel = buildModel()

            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
            assertTrue(viewModel.lists.isEmpty())
        }

    /** Pull-to-refresh discards pagination progress and re-fetches page one, not the next page. */
    @Test
    fun testRefreshReplacesListsFromPageOneRatherThanPaginating() =
        runTest(testDispatcher) {
            fakeListsRepository.listsResult =
                Result.success(
                    TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 1, name = "Old List")), totalPages = 2),
                )
            val viewModel = buildModel()
            assertEquals(ListState.IDLE, viewModel.listState)

            fakeListsRepository.listsResult =
                Result.success(
                    TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 2, name = "New List")), totalPages = 1),
                )
            viewModel.refresh()

            assertEquals(1, viewModel.lists.size)
            assertEquals("New List", viewModel.lists.first().name)
            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
        }
}
