package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.db.TrackedMedia
import com.ajinkyabadve.kmmmywatchlist.db.TrackedMediaOutbox
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TrackedMediaSnapshot
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import com.ajinkyabadve.kmmmywatchlist.network.HttpExceptionsTestFactory
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The offline favorite/watchlist outbox (KNOWN_BUGS "Offline favorite/watchlist changes are never
 * sent to TMDB"), against a real in-memory database: queued toggles reach TMDB before a refresh
 * prunes, survive while still offline, and fall back to TMDB's state when TMDB rejects them.
 */
@OptIn(ExperimentalPagingApi::class)
class TrackedMediaOutboxTest {
    private val favorite = AccountMediaCategory.FAVORITES.storageValue

    @Test
    fun testRefreshAll_sendsAQueuedRemovalAndDeletesTheHiddenRow() =
        runTest {
            val database = createTestDatabase()
            val fake = FakeAccountMediaRepository()
            val repository = repository(database, fake)
            repository.trackLocally(snapshot(), AccountMediaCategory.FAVORITES)
            removeOffline(repository)

            repository.refreshAll(ACCOUNT_ID, SESSION_ID)

            assertEquals(listOf(false), fake.setFavoriteCalls)
            assertNull(row(database))
            assertTrue(outbox(database).isEmpty())
        }

    @Test
    fun testRefreshAll_sendsAQueuedAdditionAndKeepsTheRow() =
        runTest {
            val database = createTestDatabase()
            // After the send, TMDB's favorites include the title.
            val fake =
                FakeAccountMediaRepository().apply {
                    favoriteMoviesResult = Result.success(page(SearchResultItem(id = TRACKED_ID, title = TRACKED_TITLE)))
                }
            val repository = repository(database, fake)
            addOffline(repository)

            repository.refreshAll(ACCOUNT_ID, SESSION_ID)

            assertEquals(listOf(true), fake.setFavoriteCalls)
            assertEquals(0L, row(database)?.isDeleted)
            assertTrue(outbox(database).isEmpty())
        }

    /** The send still fails, but the list fetch succeeds: the queued addition must not be pruned. */
    @Test
    fun testRefreshAll_stillOfflineKeepsAQueuedAdditionThroughThePrune() =
        runTest {
            val database = createTestDatabase()
            val fake = FakeAccountMediaRepository().apply { setFavoriteResult = Result.failure(IOException(OFFLINE)) }
            val repository = repository(database, fake)
            addOffline(repository)

            repository.refreshAll(ACCOUNT_ID, SESSION_ID)

            assertEquals(TRACKED_TITLE, row(database)?.title)
            assertEquals(1, outbox(database).size)
        }

    @Test
    fun testRefreshAll_stillOfflineStopsAtTheFirstFailureAndKeepsEveryEntry() =
        runTest {
            val database = createTestDatabase()
            val fake = FakeAccountMediaRepository().apply { setFavoriteResult = Result.failure(IOException(OFFLINE)) }
            val repository = repository(database, fake)
            repository.queueOfflineToggle(TRACKED_ID, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES, ACCOUNT_ID, isOn = true)
            repository.queueOfflineToggle(
                OTHER_TRACKED_ID,
                MediaTypeConstant.MOVIE,
                AccountMediaCategory.FAVORITES,
                ACCOUNT_ID,
                isOn = true,
            )

            repository.refreshAll(ACCOUNT_ID, SESSION_ID)

            assertEquals(1, fake.setFavoriteCalls.size)
            assertEquals(2, outbox(database).size)
        }

    @Test
    fun testRefreshAll_rejectedRemovalIsDroppedAndTheTitleShowsAgain() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeAccountMediaRepository().apply {
                    setFavoriteResult = Result.failure(HttpExceptionsTestFactory.create(HttpStatusCode.NotFound))
                    // TMDB still has it - the removal never applied.
                    favoriteMoviesResult = Result.success(page(SearchResultItem(id = TRACKED_ID, title = TRACKED_TITLE)))
                }
            val repository = repository(database, fake)
            repository.trackLocally(snapshot(), AccountMediaCategory.FAVORITES)
            removeOffline(repository)

            repository.refreshAll(ACCOUNT_ID, SESSION_ID)

            assertEquals(0L, row(database)?.isDeleted)
            assertTrue(outbox(database).isEmpty())
        }

    @Test
    fun testRefreshAll_expiredSessionKeepsTheQueueForTheNextSignIn() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeAccountMediaRepository().apply {
                    setFavoriteResult = Result.failure(HttpExceptionsTestFactory.create(HttpStatusCode.Unauthorized))
                }
            val repository = repository(database, fake)
            repository.trackLocally(snapshot(), AccountMediaCategory.FAVORITES)
            removeOffline(repository)

            repository.refreshAll(ACCOUNT_ID, SESSION_ID)

            assertEquals(1L, row(database)?.isDeleted)
            assertEquals(1, outbox(database).size)
        }

    @Test
    fun testQueueOfflineToggle_laterToggleReplacesTheEarlierOne() =
        runTest {
            val database = createTestDatabase()
            val repository = repository(database)

            repository.queueOfflineToggle(TRACKED_ID, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES, ACCOUNT_ID, isOn = false)
            repository.queueOfflineToggle(TRACKED_ID, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES, ACCOUNT_ID, isOn = true)

            assertEquals(listOf(TrackedMediaOutboxConstant.DESIRED_ADD), outbox(database).map { it.desiredState })
        }

    @Test
    fun testClearQueuedToggle_dropsTheEntry() =
        runTest {
            val database = createTestDatabase()
            val repository = repository(database)
            repository.queueOfflineToggle(TRACKED_ID, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES, ACCOUNT_ID, isOn = true)

            repository.clearQueuedToggle(TRACKED_ID, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES, ACCOUNT_ID)

            assertTrue(outbox(database).isEmpty())
        }

    @Test
    fun testRefreshAll_leavesAnotherAccountsQueueAlone() =
        runTest {
            val database = createTestDatabase()
            val fake = FakeAccountMediaRepository()
            val repository = repository(database, fake)
            repository.queueOfflineToggle(
                TRACKED_ID,
                MediaTypeConstant.MOVIE,
                AccountMediaCategory.FAVORITES,
                OTHER_ACCOUNT_ID,
                isOn = true,
            )

            repository.refreshAll(ACCOUNT_ID, SESSION_ID)

            assertTrue(fake.setFavoriteCalls.isEmpty())
            assertEquals(1, outbox(database, OTHER_ACCOUNT_ID).size)
        }

    /** Entries 2.sqm converted from pre-outbox pendingSync rows have no account (0). */
    @Test
    fun testRefreshAll_sendsUnclaimedEntriesForTheSignedInAccount() =
        runTest {
            val database = createTestDatabase()
            val fake = FakeAccountMediaRepository()
            val repository = repository(database, fake)
            repository.trackLocally(snapshot(), AccountMediaCategory.FAVORITES)
            repository.markPendingDelete(TRACKED_ID, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)
            database.myDatabaseQueries.upsertOutboxEntry(
                id = TRACKED_ID.toLong(),
                mediaType = MediaTypeConstant.MOVIE,
                category = favorite,
                accountId = UNCLAIMED_ACCOUNT_ID,
                desiredState = TrackedMediaOutboxConstant.DESIRED_REMOVE,
                queuedAt = 0,
            )

            repository.refreshAll(ACCOUNT_ID, SESSION_ID)

            assertEquals(listOf(false), fake.setFavoriteCalls)
            assertNull(row(database))
            assertTrue(outbox(database).isEmpty())
        }

    @Test
    fun testWatchlistEntryIsSentThroughSetWatchlist() =
        runTest {
            val database = createTestDatabase()
            val fake = FakeAccountMediaRepository()
            val repository = repository(database, fake)
            repository.queueOfflineToggle(TRACKED_ID, MediaTypeConstant.MOVIE, AccountMediaCategory.WATCHLIST, ACCOUNT_ID, isOn = true)

            repository.refreshAll(ACCOUNT_ID, SESSION_ID)

            assertEquals(listOf(true), fake.setWatchlistCalls)
            assertTrue(fake.setFavoriteCalls.isEmpty())
        }

    /**
     * The Favorites grid's own refresh doesn't send the queue (offline, that send's retries would
     * hold its first paint) - it keeps a queued addition through its prune for refreshAll to send.
     */
    @Test
    fun testGridRefresh_keepsAQueuedAdditionWithoutSendingIt() =
        runTest {
            val database = createTestDatabase()
            // TMDB's favorites come back without the title: the addition hasn't reached it yet.
            val fake = FakeAccountMediaRepository()
            val repository = repository(database, fake)
            addOffline(repository)

            mediator(database, fake).load(LoadType.REFRESH, emptyPagingState())

            assertTrue(fake.setFavoriteCalls.isEmpty())
            assertEquals(TRACKED_TITLE, row(database)?.title)
            assertEquals(1, outbox(database).size)
        }

    /** Same for a queued removal: the row stays hidden and queued, not revived or deleted. */
    @Test
    fun testGridRefresh_keepsAQueuedRemovalHiddenWithoutSendingIt() =
        runTest {
            val database = createTestDatabase()
            // TMDB's favorites still include the title: the removal hasn't reached it yet.
            val fake =
                FakeAccountMediaRepository().apply {
                    favoriteMoviesResult = Result.success(page(SearchResultItem(id = TRACKED_ID, title = TRACKED_TITLE)))
                }
            val repository = repository(database, fake)
            repository.trackLocally(snapshot(), AccountMediaCategory.FAVORITES)
            removeOffline(repository)

            mediator(database, fake).load(LoadType.REFRESH, emptyPagingState())

            assertTrue(fake.setFavoriteCalls.isEmpty())
            assertEquals(1L, row(database)?.isDeleted)
            assertEquals(1, outbox(database).size)
        }

    private fun repository(
        database: MyDatabase,
        fake: FakeAccountMediaRepository = FakeAccountMediaRepository(),
    ) = TrackedMediaRepositoryImpl(accountMediaRepository = fake, databaseProvider = { database })

    /** What MediaActionsState does for an offline un-favorite. */
    private suspend fun removeOffline(repository: TrackedMediaRepository) {
        repository.markPendingDelete(TRACKED_ID, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)
        repository.queueOfflineToggle(TRACKED_ID, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES, ACCOUNT_ID, isOn = false)
    }

    /** What MediaActionsState does for an offline favorite. */
    private suspend fun addOffline(repository: TrackedMediaRepository) {
        repository.trackLocally(snapshot(), AccountMediaCategory.FAVORITES)
        repository.queueOfflineToggle(TRACKED_ID, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES, ACCOUNT_ID, isOn = true)
    }

    private suspend fun row(database: MyDatabase): TrackedMedia? =
        database.myDatabaseQueries
            .selectTrackedMediaRow(TRACKED_ID.toLong(), MediaTypeConstant.MOVIE, favorite)
            .awaitAsOneOrNull()

    private suspend fun outbox(
        database: MyDatabase,
        accountId: Long = ACCOUNT_ID,
    ): List<TrackedMediaOutbox> = database.myDatabaseQueries.selectOutboxEntriesForAccount(accountId).awaitAsList()

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

    private fun emptyPagingState() =
        PagingState<Int, TrackedMedia>(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = PAGE_SIZE),
            leadingPlaceholderCount = 0,
        )

    private fun snapshot() =
        TrackedMediaSnapshot(
            id = TRACKED_ID,
            mediaType = MediaTypeConstant.MOVIE,
            title = TRACKED_TITLE,
            posterPath = null,
            releaseDate = null,
            voteAverage = 0.0,
        )

    private fun page(item: SearchResultItem) = SearchPageResult(page = 1, list = listOf(item), totalPages = 1)

    private companion object {
        const val ACCOUNT_ID = 1L
        const val OTHER_ACCOUNT_ID = 2L
        const val UNCLAIMED_ACCOUNT_ID = 0L
        const val SESSION_ID = "session"
        const val QUERY_KEY = "favorite_movie"
        const val PAGE_SIZE = 20
        const val TRACKED_ID = 33
        const val OTHER_TRACKED_ID = 44
        const val TRACKED_TITLE = "Favorited Offline"
        const val OFFLINE = "offline"
    }
}
