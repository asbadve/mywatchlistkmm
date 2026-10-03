package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Repository-level behavior not already covered by [TrackedMediaRemoteMediatorTest] (upsert/delete
 * reconciliation on refresh/append) - the pending-delete dance and poll-state updates, which are
 * plain single-row SQL calls the RemoteMediator never exercises.
 */
class TrackedMediaRepositoryImplTest {
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

    private suspend fun seedMovie(
        database: MyDatabase,
        id: Long,
        title: String,
        releaseDate: String?,
        category: String = "favorite",
    ) {
        database.myDatabaseQueries.upsertTrackedMedia(
            id = id,
            mediaType = MediaTypeConstant.MOVIE,
            category = category,
            title = title,
            posterPath = null,
            releaseDate = releaseDate,
            voteAverage = 0.0,
            addedAt = 0,
            lastSyncedAt = 0,
        )
    }

    private suspend fun seedTv(
        database: MyDatabase,
        id: Long,
        title: String,
        nextEpisodeAirDate: String?,
        category: String = "favorite",
    ) {
        database.myDatabaseQueries.upsertTrackedMedia(
            id = id,
            mediaType = MediaTypeConstant.TV,
            category = category,
            title = title,
            posterPath = null,
            releaseDate = null,
            voteAverage = 0.0,
            addedAt = 0,
            lastSyncedAt = 0,
        )
        database.myDatabaseQueries.updatePollStateForMediaType(nextEpisodeAirDate, id, MediaTypeConstant.TV)
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

    @Test
    fun testObserveUpcoming_excludesMoviePastItsReleaseDate() =
        runTest {
            val database = createTestDatabase()
            seedMovie(database, id = 1, title = "Old Movie", releaseDate = PAST_DATE)
            val repository = repository(database)

            assertTrue(repository.observeUpcoming().first().isEmpty())
        }

    @Test
    fun testObserveUpcoming_sortsMoviesByDateAscending() =
        runTest {
            val database = createTestDatabase()
            seedMovie(database, id = 1, title = "Later Movie", releaseDate = FAR_FUTURE_DATE)
            seedMovie(database, id = 2, title = "Sooner Movie", releaseDate = NEAR_FUTURE_DATE)
            val repository = repository(database)

            val items = repository.observeUpcoming().first()

            assertEquals(listOf("Sooner Movie", "Later Movie"), items.map { it.title })
        }

    @Test
    fun testObserveUpcoming_neverIncludesTvRows() =
        runTest {
            val database = createTestDatabase()
            seedTv(database, id = 1, title = "Some Show", nextEpisodeAirDate = NEAR_FUTURE_DATE)
            val repository = repository(database)

            // TV is covered by observeTrackedTvShows + cached season data instead - see
            // selectUpcomingTrackedMedia's kdoc.
            assertTrue(repository.observeUpcoming().first().isEmpty())
        }

    @Test
    fun testObserveTrackedTvShows_listsTrackedShowsRegardlessOfPollState() =
        runTest {
            val database = createTestDatabase()
            seedTv(database, id = 1, title = "Never Polled Show", nextEpisodeAirDate = null)
            val repository = repository(database)

            val shows = repository.observeTrackedTvShows().first()

            assertEquals(listOf("Never Polled Show"), shows.map { it.title })
        }

    @Test
    fun testObserveTrackedTvShows_excludesMoviesAndDeletedRows() =
        runTest {
            val database = createTestDatabase()
            seedMovie(database, id = 1, title = "A Movie", releaseDate = NEAR_FUTURE_DATE)
            seedTv(database, id = 2, title = "Removed Show", nextEpisodeAirDate = NEAR_FUTURE_DATE)
            val repository = repository(database)
            repository.markPendingDelete(2, MediaTypeConstant.TV, AccountMediaCategory.FAVORITES)

            assertTrue(repository.observeTrackedTvShows().first().isEmpty())
        }

    @Test
    fun testObserveTrackedTvShows_collapsesAShowTrackedUnderBothCategoriesToOneRow() =
        runTest {
            val database = createTestDatabase()
            seedTv(database, id = 1, title = "Dual Tracked Show", nextEpisodeAirDate = NEAR_FUTURE_DATE, category = "favorite")
            seedTv(database, id = 1, title = "Dual Tracked Show", nextEpisodeAirDate = NEAR_FUTURE_DATE, category = "watchlist")
            val repository = repository(database)

            assertEquals(1, repository.observeTrackedTvShows().first().size)
        }

    @Test
    fun testObserveUpcoming_collapsesATitleTrackedUnderBothCategoriesToOneRow() =
        runTest {
            val database = createTestDatabase()
            seedMovie(database, id = 1, title = "Dual Tracked Movie", releaseDate = NEAR_FUTURE_DATE, category = "favorite")
            seedMovie(database, id = 1, title = "Dual Tracked Movie", releaseDate = NEAR_FUTURE_DATE, category = "watchlist")
            val repository = repository(database)

            assertEquals(1, repository.observeUpcoming().first().size)
        }

    @Test
    fun testObserveUpcoming_collapsesDuplicateIdEvenWhenTitleOrPosterPathDisagree() =
        runTest {
            // Reproduces a real crash (confirmed 2026-09-28, desktop): a movie tracked under both
            // favorite and watchlist whose two rows had drifted out of sync (synced at different
            // times) so title/posterPath differ between them - SQL's DISTINCT only collapses rows
            // that match on every selected column, so this pair survives it as two rows with the
            // same id, which then crashed LazyColumn's duplicate-key check. observeUpcoming must
            // still collapse to one item by id alone, regardless of this drift.
            val database = createTestDatabase()
            seedMovie(database, id = 1, title = "Drifted Movie", releaseDate = NEAR_FUTURE_DATE, category = "favorite")
            seedMovie(database, id = 1, title = "Drifted Movie (stale)", releaseDate = NEAR_FUTURE_DATE, category = "watchlist")
            val repository = repository(database)

            assertEquals(1, repository.observeUpcoming().first().size)
        }

    @Test
    fun testObserveUpcoming_excludesDeletedRows() =
        runTest {
            val database = createTestDatabase()
            seedMovie(database, id = 1, title = "Removed Movie", releaseDate = NEAR_FUTURE_DATE)
            val repository = repository(database)
            repository.markPendingDelete(1, MediaTypeConstant.MOVIE, AccountMediaCategory.FAVORITES)

            assertTrue(repository.observeUpcoming().first().isEmpty())
        }

    private companion object {
        const val ACCOUNT_ID = 1L
        const val SESSION_ID = "session"

        // Comfortably past/future relative to whenever this test actually runs, so
        // observeUpcoming's real Clock.System.todayIn(...) comparison never flakes.
        const val PAST_DATE = "2000-01-01"
        const val NEAR_FUTURE_DATE = "2099-01-01"
        const val FAR_FUTURE_DATE = "2099-06-01"
    }
}
