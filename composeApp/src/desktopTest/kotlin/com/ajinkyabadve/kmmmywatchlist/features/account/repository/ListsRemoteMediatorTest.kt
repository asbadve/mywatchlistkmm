package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.ajinkyabadve.kmmmywatchlist.db.CustomList
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListPageResult
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalPagingApi::class)
class ListsRemoteMediatorTest {
    private companion object {
        const val ACCOUNT_ID = 1L
        const val SESSION_ID = "session"
        const val QUERY_KEY = "lists"
    }

    private fun emptyState() =
        PagingState<Int, CustomList>(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0,
        )

    private fun mediator(
        database: MyDatabase,
        fake: FakeListsRepository,
    ) = ListsRemoteMediator(
        accountId = ACCOUNT_ID,
        sessionId = SESSION_ID,
        listsRepository = fake,
        database = database,
    )

    @Test
    fun testRefreshUpsertsThePageAndStoresTheNextKey() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(
                            TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 1, name = "My List")), totalPages = 2),
                        )
                }

            val result = mediator(database, fake).load(LoadType.REFRESH, emptyState())

            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertTrue(!result.endOfPaginationReached)
            val rows = database.myDatabaseQueries.selectAllLists().awaitAsList()
            assertEquals(listOf("My List"), rows.map { it.name })
            val keys = database.myDatabaseQueries.selectRemoteKeys(QUERY_KEY).awaitAsOneOrNull()
            assertEquals(2L, keys?.nextPage)
        }

    @Test
    fun testRefreshReachesEndOfPaginationWhenOnlyOnePageExists() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 1, name = "My List")), totalPages = 1))
                }

            val result = mediator(database, fake).load(LoadType.REFRESH, emptyState())

            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertTrue(result.endOfPaginationReached)
            assertNull(
                database.myDatabaseQueries
                    .selectRemoteKeys(QUERY_KEY)
                    .awaitAsOneOrNull()
                    ?.nextPage,
            )
        }

    @Test
    fun testRefreshClearsPreviouslyCachedListsBeforeRepopulating() =
        runTest {
            val database = createTestDatabase()
            database.myDatabaseQueries.upsertList(
                id = 99,
                name = "Stale List",
                description = "",
                itemCount = 0,
                posterPath = null,
                lastSyncedAt = 0,
            )
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 1, name = "My List")), totalPages = 1))
                }

            mediator(database, fake).load(LoadType.REFRESH, emptyState())

            val rows = database.myDatabaseQueries.selectAllLists().awaitAsList()
            assertEquals(listOf("My List"), rows.map { it.name })
        }

    @Test
    fun testAppendFetchesTheStoredNextPage() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(TmdbListPageResult(page = 2, list = listOf(TmdbList(id = 2, name = "Second List")), totalPages = 2))
                }
            database.myDatabaseQueries.upsertRemoteKeys(QUERY_KEY, nextPage = 2, prevPage = null, nextKey = null)

            val result = mediator(database, fake).load(LoadType.APPEND, emptyState())

            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertTrue(result.endOfPaginationReached)
            val rows = database.myDatabaseQueries.selectAllLists().awaitAsList()
            assertEquals(listOf("Second List"), rows.map { it.name })
        }

    @Test
    fun testAppendWithNoStoredNextPageReachesEndOfPaginationWithoutFetching() =
        runTest {
            val database = createTestDatabase()

            val result = mediator(database, FakeListsRepository()).load(LoadType.APPEND, emptyState())

            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertTrue(result.endOfPaginationReached)
            assertEquals(
                0,
                database.myDatabaseQueries
                    .selectAllLists()
                    .awaitAsList()
                    .size,
            )
        }

    @Test
    fun testPrependAlwaysReachesEndOfPagination() =
        runTest {
            val database = createTestDatabase()
            val result = mediator(database, FakeListsRepository()).load(LoadType.PREPEND, emptyState())

            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertTrue(result.endOfPaginationReached)
        }

    @Test
    fun testNetworkFailureReturnsMediatorError() =
        runTest {
            val database = createTestDatabase()
            val fake = FakeListsRepository().apply { listsResult = Result.failure(IOException("Mock network failure")) }

            val result = mediator(database, fake).load(LoadType.REFRESH, emptyState())

            assertIs<RemoteMediator.MediatorResult.Error>(result)
        }
}
