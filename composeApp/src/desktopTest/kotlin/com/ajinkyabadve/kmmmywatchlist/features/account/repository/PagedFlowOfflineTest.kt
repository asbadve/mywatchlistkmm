package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.testing.asSnapshot
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Favorites grid offline: with saved rows, pagedFlow hands them out without waiting on (or
 * failing with) a network refresh - see TrackedMediaRemoteMediator.initialize.
 */
class PagedFlowOfflineTest {
    @Test
    fun testOfflinePagedFlowStillReturnsTheSavedRows() =
        runTest {
            val database = createTestDatabase()
            database.myDatabaseQueries.upsertTrackedMedia(
                id = SAVED_ID,
                mediaType = MediaTypeConstant.MOVIE,
                category = AccountMediaCategory.FAVORITES.storageValue,
                title = SAVED_TITLE,
                posterPath = null,
                releaseDate = null,
                voteAverage = 0.0,
                addedAt = 0,
                lastSyncedAt = 0,
            )
            val offline = FakeAccountMediaRepository().apply { favoriteMoviesResult = Result.failure(IOException(OFFLINE)) }
            val repository = TrackedMediaRepositoryImpl(accountMediaRepository = offline, databaseProvider = { database })

            val items =
                repository
                    .pagedFlow(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE, ACCOUNT_ID, SESSION_ID)
                    .asSnapshot()

            assertEquals(listOf(SAVED_TITLE), items.map { it.displayTitle })
        }

    /** Nothing saved yet (first sign-in): the grid still fetches, so it isn't stuck empty online. */
    @Test
    fun testEmptySliceStillFetchesFromTmdb() =
        runTest {
            val database = createTestDatabase()
            val online =
                FakeAccountMediaRepository().apply {
                    favoriteMoviesResult =
                        Result.success(
                            SearchPageResult(
                                page = 1,
                                list = listOf(SearchResultItem(id = SAVED_ID.toInt(), title = SAVED_TITLE)),
                                totalPages = 1,
                            ),
                        )
                }
            val repository = TrackedMediaRepositoryImpl(accountMediaRepository = online, databaseProvider = { database })

            val items =
                repository
                    .pagedFlow(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE, ACCOUNT_ID, SESSION_ID)
                    .asSnapshot()

            assertEquals(listOf(SAVED_TITLE), items.map { it.displayTitle })
        }

    private companion object {
        const val SAVED_ID = 7L
        const val SAVED_TITLE = "Saved Favorite"
        const val ACCOUNT_ID = 1L
        const val SESSION_ID = "session"
        const val OFFLINE = "offline"
    }
}
