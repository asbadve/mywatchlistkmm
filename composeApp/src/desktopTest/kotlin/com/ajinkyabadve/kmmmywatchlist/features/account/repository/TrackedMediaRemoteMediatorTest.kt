package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.db.TrackedMedia
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalPagingApi::class)
class TrackedMediaRemoteMediatorTest {
    private companion object {
        const val ACCOUNT_ID = 1L
        const val SESSION_ID = "session"
        const val QUERY_KEY = "favorite_movie"
    }

    private fun emptyState() =
        PagingState<Int, TrackedMedia>(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = 20),
            leadingPlaceholderCount = 0,
        )

    private fun mediator(
        database: MyDatabase,
        fake: FakeAccountMediaRepository,
    ) = TrackedMediaRemoteMediator(
        queryKey = QUERY_KEY,
        category = AccountMediaCategory.FAVORITES,
        mediaType = SearchMediaType.MOVIE,
        accountId = ACCOUNT_ID,
        sessionId = SESSION_ID,
        accountMediaRepository = fake,
        database = database,
    )

    @Test
    fun testRefreshUpsertsThePageAndStoresTheNextKey() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeAccountMediaRepository().apply {
                    favoriteMoviesResult =
                        Result.success(
                            SearchPageResult(page = 1, list = listOf(SearchResultItem(id = 1, title = "Movie One")), totalPages = 2),
                        )
                }

            val result = mediator(database, fake).load(LoadType.REFRESH, emptyState())

            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertTrue(!result.endOfPaginationReached)
            val rows = database.myDatabaseQueries.selectByCategoryPaged("favorite", MediaTypeConstant.MOVIE, 100, 0).awaitAsList()
            assertEquals(listOf("Movie One"), rows.map { it.title })
            val keys = database.myDatabaseQueries.selectRemoteKeys(QUERY_KEY).awaitAsOneOrNull()
            assertEquals(2L, keys?.nextPage)
        }

    @Test
    fun testRefreshReachesEndOfPaginationWhenOnlyOnePageExists() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeAccountMediaRepository().apply {
                    favoriteMoviesResult =
                        Result.success(
                            SearchPageResult(page = 1, list = listOf(SearchResultItem(id = 1, title = "Movie One")), totalPages = 1),
                        )
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
    fun testAppendFetchesTheStoredNextPage() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeAccountMediaRepository().apply {
                    favoriteMoviesResult =
                        Result.success(
                            SearchPageResult(page = 2, list = listOf(SearchResultItem(id = 2, title = "Movie Two")), totalPages = 2),
                        )
                }
            database.myDatabaseQueries.upsertRemoteKeys(QUERY_KEY, nextPage = 2, prevPage = null, nextKey = null)

            val result = mediator(database, fake).load(LoadType.APPEND, emptyState())

            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertTrue(result.endOfPaginationReached)
            val rows = database.myDatabaseQueries.selectByCategoryPaged("favorite", MediaTypeConstant.MOVIE, 100, 0).awaitAsList()
            assertEquals(listOf("Movie Two"), rows.map { it.title })
        }

    @Test
    fun testAppendWithNoStoredNextPageReachesEndOfPaginationWithoutFetching() =
        runTest {
            val database = createTestDatabase()
            val fake = FakeAccountMediaRepository()

            val result = mediator(database, fake).load(LoadType.APPEND, emptyState())

            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertTrue(result.endOfPaginationReached)
            assertEquals(
                0,
                database.myDatabaseQueries
                    .selectByCategoryPaged("favorite", MediaTypeConstant.MOVIE, 100, 0)
                    .awaitAsList()
                    .size,
            )
        }

    @Test
    fun testPrependAlwaysReachesEndOfPagination() =
        runTest {
            val database = createTestDatabase()
            val result = mediator(database, FakeAccountMediaRepository()).load(LoadType.PREPEND, emptyState())

            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertTrue(result.endOfPaginationReached)
        }

    @Test
    fun testNetworkFailureReturnsMediatorError() =
        runTest {
            val database = createTestDatabase()
            val fake = FakeAccountMediaRepository().apply { favoriteMoviesResult = Result.failure(IOException("Mock network failure")) }

            val result = mediator(database, fake).load(LoadType.REFRESH, emptyState())

            assertIs<RemoteMediator.MediatorResult.Error>(result)
        }

    /** REFRESH must not wipe a row still queued for an offline delete (pendingSync = 1). */
    @Test
    fun testRefreshPreservesARowQueuedForAnOfflineDelete() =
        runTest {
            val database = createTestDatabase()
            database.myDatabaseQueries.upsertTrackedMedia(
                id = 5,
                mediaType = MediaTypeConstant.MOVIE,
                category = "favorite",
                title = "Queued For Delete",
                posterPath = null,
                releaseDate = null,
                voteAverage = 0.0,
                addedAt = 0,
                lastSyncedAt = 0,
            )
            database.myDatabaseQueries.markTrackedMediaPendingDelete(5, MediaTypeConstant.MOVIE, "favorite")
            val fake =
                FakeAccountMediaRepository().apply {
                    favoriteMoviesResult =
                        Result.success(
                            SearchPageResult(page = 1, list = listOf(SearchResultItem(id = 1, title = "Movie One")), totalPages = 1),
                        )
                }

            mediator(database, fake).load(LoadType.REFRESH, emptyState())

            val queuedRow =
                database.myDatabaseQueries
                    .selectTrackedMediaRow(5, MediaTypeConstant.MOVIE, "favorite")
                    .awaitAsOneOrNull()
            assertEquals(1L, queuedRow?.pendingSync)
        }
}
