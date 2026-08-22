package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.paging3.QueryPagingSource
import com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider
import com.ajinkyabadve.kmmmywatchlist.db.CustomList
import com.ajinkyabadve.kmmmywatchlist.db.CustomListItem
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException

private object CustomListRepositoryConstant {
    const val TAG = "CustomListRepository"

    // TMDB's page size is fixed (not configurable via the API) - matching it exactly means each
    // RemoteMediator fetch lines up with one local Paging window instead of a partial one.
    const val PAGE_SIZE = 20
}

/**
 * Local SQLite is the single source of truth for a signed-in user's custom lists (see
 * `docs/local-storage-plan.html` and `future_features_checklist.md` item 2) - same SSOT role as
 * [TrackedMediaRepository], for [ListsRepository]'s `/3/list` data instead of favorites/watchlist.
 * [observeLists]/[observeListDetail] never talk to the network; [sync]/[refreshListDetail] fetch from
 * TMDB and write through to the local tables, which is what the two `observe*` Flows re-emit from -
 * nothing here ever writes to TMDB itself.
 */
interface CustomListRepository {
    fun observeLists(): Flow<List<TmdbList>>

    /**
     * Paging3-backed replacement for [observeLists]/[sync] on the Lists grid - unlike [sync]
     * (which always fetches every list and every list's items), this fetches only the page of
     * *list summaries* the grid has actually scrolled to, via [ListsRemoteMediator] - it never
     * fetches a list's items (that stays [refreshListDetail]'s job, on demand when a list is opened).
     */
    fun pagedFlow(
        accountId: Long,
        sessionId: String,
    ): Flow<PagingData<TmdbList>>

    /**
     * Writes [list] straight into the local table without a network round trip - for a list this
     * app just created via `ListsRepository.createList`, so it appears in [observeLists]/[pagedFlow]
     * immediately instead of waiting for the next sync/refresh to pick it up.
     */
    suspend fun upsertListLocally(list: TmdbList)

    /** Combines this list's cached name and its (non-deleted) cached items into one Flow. */
    fun observeListDetail(listId: Long): Flow<TmdbListDetail?>

    /** Fetches every list (and every list's items) and reconciles the local tables to match. */
    suspend fun sync(
        accountId: Long,
        sessionId: String,
    )

    /** Fetches one list's detail and writes it through to the local tables. */
    suspend fun refreshListDetail(
        listId: Long,
        sessionId: String,
    ): TmdbListDetail

    /**
     * Removes one list locally without a full [sync] - for callers that already know a list was
     * just deleted on TMDB (e.g. right after a successful `ListsRepository.deleteList` call) and
     * don't want to wait on a full re-fetch to reflect it.
     */
    suspend fun removeListLocally(listId: Long)

    /**
     * Hides [movieId] from [observeListDetail] for [listId] immediately and queues it for removal
     * from TMDB - call this right after a "remove from list" tap, before the network call resolves,
     * so the item drops out of the local list whether or not the device is online.
     */
    suspend fun markItemPendingDelete(
        listId: Long,
        movieId: Long,
    )

    /** Undoes [markItemPendingDelete] - for a removal that turned out to have failed outright. */
    suspend fun clearItemPendingDelete(
        listId: Long,
        movieId: Long,
    )

    /** Hard-deletes a row [markItemPendingDelete] queued, once its removal is confirmed with TMDB. */
    suspend fun confirmItemDelete(
        listId: Long,
        movieId: Long,
    )
}

/** See [createTrackedMediaRepository]'s identical kdoc - same per-platform split, for lists. */
internal expect fun createCustomListRepository(
    listsRepository: ListsRepository,
    databaseProvider: suspend () -> MyDatabase,
): CustomListRepository

class CustomListRepositoryImpl(
    listsRepository: ListsRepository = ListsRepositoryImpl(),
    // Test-only seam - see TrackedMediaRepositoryImpl's identical parameter for why this stays a
    // suspend factory rather than a plain default value.
    databaseProvider: suspend () -> MyDatabase = { AppDatabaseProvider.get() },
) : CustomListRepository by createCustomListRepository(listsRepository, databaseProvider)

internal class SqliteCustomListRepositoryImpl(
    private val listsRepository: ListsRepository,
    private val databaseProvider: suspend () -> MyDatabase,
) : CustomListRepository {
    override fun observeLists(): Flow<List<TmdbList>> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectAllLists()
                    .asFlow()
                    .mapToList(Dispatchers.Default)
                    .map { rows -> rows.map { it.toTmdbList() } },
            )
        }

    @OptIn(ExperimentalPagingApi::class)
    override fun pagedFlow(
        accountId: Long,
        sessionId: String,
    ): Flow<PagingData<TmdbList>> =
        flow {
            // Resolved once per collector, not per pagingSourceFactory call - see
            // TrackedMediaRepositoryImpl.pagedFlow's identical comment for why.
            val database = databaseProvider()
            val queries = database.myDatabaseQueries
            val pager =
                Pager(
                    // initialLoadSize defaults to pageSize * 3 - see TrackedMediaRepositoryImpl's
                    // identical comment for why this is pinned to one page instead.
                    config =
                        PagingConfig(
                            pageSize = CustomListRepositoryConstant.PAGE_SIZE,
                            initialLoadSize = CustomListRepositoryConstant.PAGE_SIZE,
                        ),
                    remoteMediator =
                        ListsRemoteMediator(
                            accountId = accountId,
                            sessionId = sessionId,
                            listsRepository = listsRepository,
                            database = database,
                        ),
                ) {
                    QueryPagingSource(
                        countQuery = queries.countLists(),
                        transacter = queries,
                        context = Dispatchers.Default,
                        queryProvider = { limit, offset -> queries.selectAllListsPaged(limit, offset) },
                    )
                }
            emitAll(pager.flow.map { pagingData -> pagingData.map { it.toTmdbList() } })
        }

    override suspend fun upsertListLocally(list: TmdbList) {
        databaseProvider().myDatabaseQueries.upsertList(
            id = list.id,
            name = list.name,
            description = list.description,
            itemCount = list.itemCount.toLong(),
            posterPath = list.posterPath,
            lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    override fun observeListDetail(listId: Long): Flow<TmdbListDetail?> =
        flow {
            val queries = databaseProvider().myDatabaseQueries
            val nameFlow = queries.selectListById(listId).asFlow().mapToOneOrNull(Dispatchers.Default)
            val itemsFlow = queries.selectItemsForList(listId).asFlow().mapToList(Dispatchers.Default)
            emitAll(
                combine(nameFlow, itemsFlow) { list, items ->
                    list?.let { TmdbListDetail(name = it.name, description = it.description, items = items.map { row -> row.toMovie() }) }
                },
            )
        }

    override suspend fun sync(
        accountId: Long,
        sessionId: String,
    ) {
        // Push any offline item removals before re-fetching - see TrackedMediaRepositoryImpl.sync's
        // identical flush-then-reconcile ordering.
        flushPendingItemDeletes(sessionId)

        val lists = fetchAllLists(accountId, sessionId)
        val queries = databaseProvider().myDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()

        val existingIds =
            queries
                .selectAllLists()
                .awaitAsList()
                .map { it.id }
                .toSet()
        val currentIds = lists.map { it.id }.toSet()
        (existingIds - currentIds).forEach { removedId -> queries.deleteListItems(removedId) }

        lists.forEach { list ->
            queries.upsertList(
                id = list.id,
                name = list.name,
                description = list.description,
                itemCount = list.itemCount.toLong(),
                posterPath = list.posterPath,
                lastSyncedAt = now,
            )
            writeListDetail(list.id, listsRepository.getListDetails(list.id, sessionId))
        }
        queries.deleteListsNotIn(currentIds.toList())
    }

    override suspend fun refreshListDetail(
        listId: Long,
        sessionId: String,
    ): TmdbListDetail {
        val detail = listsRepository.getListDetails(listId, sessionId)
        writeListDetail(listId, detail)
        return detail
    }

    private suspend fun writeListDetail(
        listId: Long,
        detail: TmdbListDetail,
    ) {
        val queries = databaseProvider().myDatabaseQueries
        detail.items.forEachIndexed { index, movie ->
            queries.replaceListItem(
                listId = listId,
                movieId = movie.id.toLong(),
                title = movie.title,
                posterPath = movie.posterPath,
                releaseDate = movie.releaseDate,
                voteAverage = movie.voteAverage,
                overview = movie.overview,
                position = index.toLong(),
            )
        }
        queries.deleteListItemsNotIn(listId, detail.items.map { it.id.toLong() })
    }

    override suspend fun removeListLocally(listId: Long) {
        val queries = databaseProvider().myDatabaseQueries
        queries.deleteListItems(listId)
        queries.deleteList(listId)
    }

    override suspend fun markItemPendingDelete(
        listId: Long,
        movieId: Long,
    ) {
        databaseProvider().myDatabaseQueries.markListItemPendingDelete(listId, movieId)
    }

    override suspend fun clearItemPendingDelete(
        listId: Long,
        movieId: Long,
    ) {
        databaseProvider().myDatabaseQueries.clearListItemPendingDelete(listId, movieId)
    }

    override suspend fun confirmItemDelete(
        listId: Long,
        movieId: Long,
    ) {
        databaseProvider().myDatabaseQueries.deleteListItemRow(listId, movieId)
    }

    private suspend fun flushPendingItemDeletes(sessionId: String) {
        val queries = databaseProvider().myDatabaseQueries
        queries.selectPendingSyncListItems().awaitAsList().forEach { row ->
            try {
                listsRepository.removeMovieFromList(row.listId, sessionId, row.movieId)
                queries.deleteListItemRow(row.listId, row.movieId)
            } catch (e: HttpExceptions) {
                Napier.e(tag = CustomListRepositoryConstant.TAG, throwable = e) {
                    "Http error flushing pending delete for movie ${row.movieId} in list ${row.listId}"
                }
            } catch (e: IOException) {
                Napier.e(tag = CustomListRepositoryConstant.TAG, throwable = e) {
                    "Network error flushing pending delete for movie ${row.movieId} in list ${row.listId}"
                }
            } catch (e: ContentConvertException) {
                Napier.e(tag = CustomListRepositoryConstant.TAG, throwable = e) {
                    "Malformed response flushing pending delete for movie ${row.movieId} in list ${row.listId}"
                }
            } catch (e: SerializationException) {
                Napier.e(tag = CustomListRepositoryConstant.TAG, throwable = e) {
                    "Malformed response flushing pending delete for movie ${row.movieId} in list ${row.listId}"
                }
            }
        }
    }

    private suspend fun fetchAllLists(
        accountId: Long,
        sessionId: String,
    ): List<TmdbList> {
        val lists = mutableListOf<TmdbList>()
        var page = 1
        var totalPages = 1
        do {
            val response = listsRepository.getLists(accountId, sessionId, page)
            response.list?.let { lists.addAll(it) }
            totalPages = response.totalPages ?: page
            page++
        } while (page <= totalPages)
        return lists
    }
}

private fun CustomList.toTmdbList(): TmdbList =
    TmdbList(
        id = id,
        name = name,
        description = description,
        itemCount = itemCount.toInt(),
        posterPath = posterPath,
    )

@OptIn(ExperimentalSerializationApi::class)
private fun CustomListItem.toMovie(): Movie =
    Movie(
        id = movieId.toInt(),
        title = title,
        posterPath = posterPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        overview = overview,
    )
