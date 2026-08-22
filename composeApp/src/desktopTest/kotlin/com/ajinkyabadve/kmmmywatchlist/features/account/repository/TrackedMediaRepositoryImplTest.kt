package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Repository-level behavior not already covered by [TrackedMediaRemoteMediatorTest] (upsert/delete
 * reconciliation on refresh/append) - the pending-delete dance and poll-state updates, which are
 * plain single-row SQL calls the RemoteMediator never exercises.
 */
class TrackedMediaRepositoryImplTest {
    private companion object {
        const val ACCOUNT_ID = 1L
        const val SESSION_ID = "session"
    }

    private suspend fun repository(
        database: MyDatabase,
        fake: FakeAccountMediaRepository = FakeAccountMediaRepository(),
    ) = TrackedMediaRepositoryImpl(accountMediaRepository = fake, databaseProvider = { database })

    private suspend fun seedRow(
        database: MyDatabase,
        id: Long = 1,
    ) {
        database.myDatabaseQueries.upsertTrackedMedia(
            id = id,
            mediaType = MediaTypeConstant.MOVIE,
            category = "favorite",
            title = "Movie One",
            posterPath = null,
            releaseDate = null,
            voteAverage = 0.0,
            addedAt = 0,
            lastSyncedAt = 0,
        )
    }

    @Test
    fun testMarkPendingDeleteHidesTheItemImmediately() =
        runTest {
            val database = createTestDatabase()
            seedRow(database)
            val repository = repository(database)

            repository.markPendingDelete(1, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)

            val row = database.myDatabaseQueries.selectTrackedMediaRow(1, MediaTypeConstant.MOVIE, "favorite").awaitAsOneOrNull()
            assertEquals(1L, row?.isDeleted)
            assertEquals(1L, row?.pendingSync)
        }

    @Test
    fun testClearPendingDeleteRestoresVisibility() =
        runTest {
            val database = createTestDatabase()
            seedRow(database)
            val repository = repository(database)
            repository.markPendingDelete(1, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)

            repository.clearPendingDelete(1, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)

            val row = database.myDatabaseQueries.selectTrackedMediaRow(1, MediaTypeConstant.MOVIE, "favorite").awaitAsOneOrNull()
            assertEquals(0L, row?.isDeleted)
            assertEquals(0L, row?.pendingSync)
        }

    @Test
    fun testConfirmDeleteHardDeletesTheRow() =
        runTest {
            val database = createTestDatabase()
            seedRow(database)
            val repository = repository(database)
            repository.markPendingDelete(1, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)

            repository.confirmDelete(1, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)

            assertNull(database.myDatabaseQueries.selectTrackedMediaRow(1, MediaTypeConstant.MOVIE, "favorite").awaitAsOneOrNull())
        }

    @Test
    fun testUpdatePollStateSetsPollFieldsWithoutTouchingOtherColumns() =
        runTest {
            val database = createTestDatabase()
            seedRow(database)
            val repository = repository(database)

            repository.updatePollState(
                id = 1,
                mediaType = MediaTypeConstant.MOVIE,
                category = AccountMediaCategory.FAVORITES,
                nextEpisodeAirDate = "2026-09-01",
                creditIds = "5,6,7",
            )

            val row = database.myDatabaseQueries.selectTrackedMediaRow(1, MediaTypeConstant.MOVIE, "favorite").awaitAsOneOrNull()
            assertEquals("2026-09-01", row?.lastKnownNextEpisodeAirDate)
            assertEquals("5,6,7", row?.lastKnownCreditIds)
            assertEquals("Movie One", row?.title)
        }
}
