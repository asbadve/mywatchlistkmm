package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

private object NetworkOnlyCustomListConstant {
    // TMDB's page size is fixed (not configurable via the API).
    const val PAGE_SIZE = 20
}

private data class ListItemRow(
    val movieId: Int,
    val movie: Movie,
    val pendingDelete: Boolean = false,
)

/**
 * Web-only stand-in for [SqliteCustomListRepositoryImpl] - see
 * [NetworkOnlyTrackedMediaRepositoryImpl]'s identical kdoc for why. An in-memory-only mirror, not
 * a SQLite table: nothing here survives a page reload, and [pagedFlow] fetches TMDB directly
 * rather than reading this mirror.
 */
internal class NetworkOnlyCustomListRepositoryImpl(
    private val listsRepository: ListsRepository,
) : CustomListRepository {
    private val lists = MutableStateFlow<List<TmdbList>>(emptyList())
    private val listItems = MutableStateFlow<Map<Long, List<ListItemRow>>>(emptyMap())

    override fun observeLists(): Flow<List<TmdbList>> = lists

    override fun pagedFlow(
        accountId: Long,
        sessionId: String,
    ): Flow<PagingData<TmdbList>> =
        // initialLoadSize defaults to pageSize * 3 - see SqliteTrackedMediaRepositoryImpl's
        // identical comment for why this is pinned to one page instead.
        Pager(
            PagingConfig(
                pageSize = NetworkOnlyCustomListConstant.PAGE_SIZE,
                initialLoadSize = NetworkOnlyCustomListConstant.PAGE_SIZE,
            ),
        ) {
            NetworkPagingSource { page ->
                val response = listsRepository.getLists(accountId, sessionId, page)
                response.list.orEmpty() to response.totalPages
            }
        }.flow

    override suspend fun upsertListLocally(list: TmdbList) {
        lists.update { current -> listOf(list) + current.filterNot { it.id == list.id } }
    }

    override fun observeListDetail(listId: Long): Flow<TmdbListDetail?> =
        combine(lists, listItems) { allLists, itemsByList ->
            val list = allLists.find { it.id == listId } ?: return@combine null
            val items = itemsByList[listId].orEmpty().filterNot { it.pendingDelete }.map { it.movie }
            TmdbListDetail(name = list.name, description = list.description, items = items)
        }

    override suspend fun sync(
        accountId: Long,
        sessionId: String,
    ) {
        val fetched = fetchAllLists(accountId, sessionId)
        lists.value = fetched
        fetched.forEach { list -> writeListDetail(list.id, listsRepository.getListDetails(list.id, sessionId)) }
    }

    override suspend fun refreshListDetail(
        listId: Long,
        sessionId: String,
    ): TmdbListDetail {
        val detail = listsRepository.getListDetails(listId, sessionId)
        writeListDetail(listId, detail)
        return detail
    }

    override suspend fun removeListLocally(listId: Long) {
        lists.update { current -> current.filterNot { it.id == listId } }
        listItems.update { current -> current - listId }
    }

    override suspend fun markItemPendingDelete(
        listId: Long,
        movieId: Long,
    ) {
        updateItem(listId, movieId) { it.copy(pendingDelete = true) }
    }

    override suspend fun clearItemPendingDelete(
        listId: Long,
        movieId: Long,
    ) {
        updateItem(listId, movieId) { it.copy(pendingDelete = false) }
    }

    override suspend fun confirmItemDelete(
        listId: Long,
        movieId: Long,
    ) {
        listItems.update { current ->
            current + (listId to current[listId].orEmpty().filterNot { it.movieId == movieId.toInt() })
        }
    }

    private fun updateItem(
        listId: Long,
        movieId: Long,
        transform: (ListItemRow) -> ListItemRow,
    ) {
        listItems.update { current ->
            val items = current[listId].orEmpty().map { row -> if (row.movieId == movieId.toInt()) transform(row) else row }
            current + (listId to items)
        }
    }

    private fun writeListDetail(
        listId: Long,
        detail: TmdbListDetail,
    ) {
        listItems.update { current -> current + (listId to detail.items.map { ListItemRow(it.id, it) }) }
    }

    private suspend fun fetchAllLists(
        accountId: Long,
        sessionId: String,
    ): List<TmdbList> {
        val result = mutableListOf<TmdbList>()
        var page = 1
        var totalPages = 1
        do {
            val response = listsRepository.getLists(accountId, sessionId, page)
            response.list?.let { result.addAll(it) }
            totalPages = response.totalPages ?: page
            page++
        } while (page <= totalPages)
        return result
    }
}
