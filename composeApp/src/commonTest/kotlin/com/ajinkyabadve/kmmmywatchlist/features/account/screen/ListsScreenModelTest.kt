package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeCustomListRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeListsRepository
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

private object ListsScreenModelTestConstant {
    const val ACCOUNT_ID = 100L
    const val SESSION_ID = "session_abc"
}

/**
 * Pagination/network-fetch behavior for the Lists grid now lives in
 * `CustomListRepository.pagedFlow`/`ListsRemoteMediator`, covered against a real database in
 * `desktopTest`'s `ListsRemoteMediatorTest` - this ScreenModel only wires `accountId`/`sessionId`
 * through to the repository (mirrors `AccountMediaListScreenModelTest`'s identical scope), plus
 * `createList`/`ensureAllListsAreSynced`, which stay ScreenModel-owned since they're not paging.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListsScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeListsRepository = FakeListsRepository()
    private val fakeCustomListRepository = FakeCustomListRepository()

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
            customListRepository = fakeCustomListRepository,
        )

    @Test
    fun testConstructionCallsPagedFlowWithAccountIdAndSessionId() {
        buildModel()

        assertEquals(
            listOf(ListsScreenModelTestConstant.ACCOUNT_ID to ListsScreenModelTestConstant.SESSION_ID),
            fakeCustomListRepository.pagedFlowCalls,
        )
    }

    @Test
    fun testCreateListAddsNewListLocallyAndInvokesCallback() =
        runTest(testDispatcher) {
            fakeListsRepository.createListResult = Result.success(555L)
            val viewModel = buildModel()

            var createdListId: Long? = null
            viewModel.createList("New List", "desc") { listId -> createdListId = listId }

            assertEquals(555L, createdListId)
            assertEquals(listOf(555L), fakeCustomListRepository.upsertListLocallyCalls.map { it.id })
            assertIs<CreateListState.Idle>(viewModel.createListState)
        }

    @Test
    fun testCreateListFailureSetsErrorState() =
        runTest(testDispatcher) {
            fakeListsRepository.createListResult = Result.failure(IOException("Mock network failure"))
            val viewModel = buildModel()

            viewModel.createList("New List", "desc") {}

            assertIs<CreateListState.Error>(viewModel.createListState)
        }

    @Test
    fun testEnsureAllListsAreSyncedCallsRepositorySync() =
        runTest(testDispatcher) {
            val viewModel = buildModel()

            viewModel.ensureAllListsAreSynced()

            assertEquals(
                listOf(ListsScreenModelTestConstant.ACCOUNT_ID to ListsScreenModelTestConstant.SESSION_ID),
                fakeCustomListRepository.syncCalls,
            )
        }
}
