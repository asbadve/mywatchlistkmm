package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListPageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class CustomListRepositoryImplTest {
    private companion object {
        const val ACCOUNT_ID = 1L
        const val SESSION_ID = "session"
    }

    @Test
    fun testSyncUpsertsListsAndTheirItems() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(
                            TmdbListPageResult(
                                page = 1,
                                list = listOf(TmdbList(id = 10, name = "My List", description = "desc", itemCount = 1)),
                                totalPages = 1,
                            ),
                        )
                    listDetailsResult =
                        Result.success(
                            TmdbListDetail(
                                name = "My List",
                                description = "desc",
                                items = listOf(Movie(id = 100, title = "Movie One", releaseDate = "2020-01-01", voteAverage = 7.5)),
                            ),
                        )
                }
            val repository = CustomListRepositoryImpl(listsRepository = fake, databaseProvider = { database })

            repository.sync(ACCOUNT_ID, SESSION_ID)

            assertEquals(listOf("My List"), repository.observeLists().first().map { it.name })
            assertEquals(listOf("Movie One"), (repository.observeListDetail(10).first()?.items ?: emptyList()).map { it.title })
        }

    @Test
    fun testSyncRemovesListsAndTheirItemsWhenNoLongerReturnedByTmdb() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(
                            TmdbListPageResult(
                                page = 1,
                                list = listOf(TmdbList(id = 10, name = "My List", description = "", itemCount = 1)),
                                totalPages = 1,
                            ),
                        )
                    listDetailsResult =
                        Result.success(
                            TmdbListDetail(name = "My List", description = "", items = listOf(Movie(id = 100, title = "Movie One"))),
                        )
                }
            val repository = CustomListRepositoryImpl(listsRepository = fake, databaseProvider = { database })
            repository.sync(ACCOUNT_ID, SESSION_ID)
            assertEquals(1, repository.observeLists().first().size)

            // The list was deleted on TMDB between syncs.
            fake.listsResult = Result.success(TmdbListPageResult(page = 1, list = emptyList(), totalPages = 1))
            repository.sync(ACCOUNT_ID, SESSION_ID)

            assertTrue(repository.observeLists().first().isEmpty())
            assertTrue((repository.observeListDetail(10).first()?.items ?: emptyList()).isEmpty())
        }

    @Test
    fun testRemoveListLocallyDropsTheListAndItsItemsWithoutANetworkCall() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(
                            TmdbListPageResult(
                                page = 1,
                                list = listOf(TmdbList(id = 10, name = "My List", description = "", itemCount = 1)),
                                totalPages = 1,
                            ),
                        )
                    listDetailsResult =
                        Result.success(
                            TmdbListDetail(name = "My List", description = "", items = listOf(Movie(id = 100, title = "Movie One"))),
                        )
                }
            val repository = CustomListRepositoryImpl(listsRepository = fake, databaseProvider = { database })
            repository.sync(ACCOUNT_ID, SESSION_ID)

            repository.removeListLocally(10)

            assertTrue(repository.observeLists().first().isEmpty())
            assertTrue((repository.observeListDetail(10).first()?.items ?: emptyList()).isEmpty())
        }

    @Test
    fun testItemPositionIsPreservedInTmdbsOriginalOrder() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(
                            TmdbListPageResult(
                                page = 1,
                                list = listOf(TmdbList(id = 10, name = "My List", description = "", itemCount = 2)),
                                totalPages = 1,
                            ),
                        )
                    listDetailsResult =
                        Result.success(
                            TmdbListDetail(
                                name = "My List",
                                description = "",
                                items = listOf(Movie(id = 200, title = "Second Added"), Movie(id = 100, title = "First Added")),
                            ),
                        )
                }
            val repository = CustomListRepositoryImpl(listsRepository = fake, databaseProvider = { database })

            repository.sync(ACCOUNT_ID, SESSION_ID)

            assertEquals(
                listOf("Second Added", "First Added"),
                (repository.observeListDetail(10).first()?.items ?: emptyList()).map { it.title },
            )
        }

    @Test
    fun testMarkItemPendingDeleteHidesTheItemImmediately() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(
                            TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 10, name = "My List", itemCount = 1)), totalPages = 1),
                        )
                    listDetailsResult =
                        Result.success(TmdbListDetail(name = "My List", items = listOf(Movie(id = 100, title = "Movie One"))))
                }
            val repository = CustomListRepositoryImpl(listsRepository = fake, databaseProvider = { database })
            repository.sync(ACCOUNT_ID, SESSION_ID)

            repository.markItemPendingDelete(10, 100)

            assertTrue((repository.observeListDetail(10).first()?.items ?: emptyList()).isEmpty())
        }

    @Test
    fun testClearItemPendingDeleteRestoresVisibility() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(
                            TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 10, name = "My List", itemCount = 1)), totalPages = 1),
                        )
                    listDetailsResult =
                        Result.success(TmdbListDetail(name = "My List", items = listOf(Movie(id = 100, title = "Movie One"))))
                }
            val repository = CustomListRepositoryImpl(listsRepository = fake, databaseProvider = { database })
            repository.sync(ACCOUNT_ID, SESSION_ID)
            repository.markItemPendingDelete(10, 100)

            repository.clearItemPendingDelete(10, 100)

            assertEquals(listOf("Movie One"), (repository.observeListDetail(10).first()?.items ?: emptyList()).map { it.title })
        }

    @Test
    fun testConfirmItemDeleteHardDeletesTheRow() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(
                            TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 10, name = "My List", itemCount = 1)), totalPages = 1),
                        )
                    listDetailsResult =
                        Result.success(TmdbListDetail(name = "My List", items = listOf(Movie(id = 100, title = "Movie One"))))
                }
            val repository = CustomListRepositoryImpl(listsRepository = fake, databaseProvider = { database })
            repository.sync(ACCOUNT_ID, SESSION_ID)
            repository.markItemPendingDelete(10, 100)

            repository.confirmItemDelete(10, 100)

            assertTrue((repository.observeListDetail(10).first()?.items ?: emptyList()).isEmpty())
        }

    /** The next sync() must push a queued offline item removal to TMDB before reconciling. */
    @Test
    fun testSyncFlushesAQueuedItemDeleteBeforeReconciling() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(
                            TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 10, name = "My List", itemCount = 1)), totalPages = 1),
                        )
                    listDetailsResult =
                        Result.success(TmdbListDetail(name = "My List", items = listOf(Movie(id = 100, title = "Movie One"))))
                }
            val repository = CustomListRepositoryImpl(listsRepository = fake, databaseProvider = { database })
            repository.sync(ACCOUNT_ID, SESSION_ID)
            repository.markItemPendingDelete(10, 100)

            // The fake endpoint keeps returning the item - this proves the flush step itself ran
            // and pushed the queued removal, not what the local table looks like afterwards.
            repository.sync(ACCOUNT_ID, SESSION_ID)

            assertEquals(listOf(100L), fake.removeMovieFromListCalls)
        }

    /** A resync must not silently wipe a queued item delete before its flush ever got a chance to run. */
    @Test
    fun testResyncDoesNotResurrectOrLoseAPendingItemDelete() =
        runTest {
            val database = createTestDatabase()
            val fake =
                FakeListsRepository().apply {
                    listsResult =
                        Result.success(
                            TmdbListPageResult(
                                page = 1,
                                list = listOf(TmdbList(id = 10, name = "My List", itemCount = 2)),
                                totalPages = 1,
                            ),
                        )
                    listDetailsResult =
                        Result.success(
                            TmdbListDetail(
                                name = "My List",
                                items = listOf(Movie(id = 100, title = "Movie One"), Movie(id = 200, title = "Movie Two")),
                            ),
                        )
                    // The pending item's own removal call fails outright, so the row survives this
                    // flush attempt (an offline failure would look the same from here).
                    removeMovieFromListResult =
                        Result.failure(
                            io.ktor.utils.io.errors
                                .IOException("Mock network failure"),
                        )
                }
            val repository = CustomListRepositoryImpl(listsRepository = fake, databaseProvider = { database })
            repository.sync(ACCOUNT_ID, SESSION_ID)
            repository.markItemPendingDelete(10, 100)

            repository.sync(ACCOUNT_ID, SESSION_ID)

            assertEquals(listOf("Movie Two"), (repository.observeListDetail(10).first()?.items ?: emptyList()).map { it.title })
        }
}
