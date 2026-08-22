package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.PagingData
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-in for [CustomListRepository] - same reasoning as [FakeTrackedMediaRepository]:
 * screen model tests never touch the real platform database. [CustomListRepositoryImpl]'s own
 * upsert/delete/position behavior is covered against a real in-memory SQLite database in
 * `desktopTest`'s `CustomListRepositoryImplTest` instead. [markItemPendingDelete]/
 * [clearItemPendingDelete]/[confirmItemDelete] mirror the real `isDeleted`/`pendingSync` semantics
 * closely enough that [observeListDetail]'s Flow reacts the same way the real DB-backed one does -
 * ScreenModel tests rely on that reactivity instead of manually inspecting call logs.
 */
class FakeCustomListRepository : CustomListRepository {
    private val listsFlow = MutableStateFlow<List<TmdbList>>(emptyList())
    private val baseDetails = mutableMapOf<Long, TmdbListDetail>()
    private val hiddenItemIds = mutableMapOf<Long, MutableSet<Long>>()
    private val detailFlows = mutableMapOf<Long, MutableStateFlow<TmdbListDetail?>>()
    var refreshListDetailResult: Result<TmdbListDetail> = Result.success(TmdbListDetail())
    val syncCalls = mutableListOf<Pair<Long, String>>()
    val refreshListDetailCalls = mutableListOf<Long>()
    val removeListLocallyCalls = mutableListOf<Long>()
    val markItemPendingDeleteCalls = mutableListOf<Pair<Long, Long>>()
    val clearItemPendingDeleteCalls = mutableListOf<Pair<Long, Long>>()
    val confirmItemDeleteCalls = mutableListOf<Pair<Long, Long>>()

    var lists: List<TmdbList>
        get() = listsFlow.value
        set(value) {
            listsFlow.value = value
        }

    fun seedListDetail(
        listId: Long,
        detail: TmdbListDetail,
    ) {
        baseDetails[listId] = detail
        recompute(listId)
    }

    override fun observeLists(): Flow<List<TmdbList>> = listsFlow

    val pagedFlowCalls = mutableListOf<Pair<Long, String>>()
    val upsertListLocallyCalls = mutableListOf<TmdbList>()

    override fun pagedFlow(
        accountId: Long,
        sessionId: String,
    ): Flow<PagingData<TmdbList>> {
        pagedFlowCalls.add(accountId to sessionId)
        return listsFlow.map { PagingData.from(it) }
    }

    override suspend fun upsertListLocally(list: TmdbList) {
        upsertListLocallyCalls.add(list)
        listsFlow.value = listsFlow.value.filterNot { it.id == list.id } + list
    }

    override fun observeListDetail(listId: Long): Flow<TmdbListDetail?> = detailFlowFor(listId)

    override suspend fun sync(
        accountId: Long,
        sessionId: String,
    ) {
        syncCalls.add(accountId to sessionId)
    }

    override suspend fun refreshListDetail(
        listId: Long,
        sessionId: String,
    ): TmdbListDetail {
        refreshListDetailCalls.add(listId)
        val detail = refreshListDetailResult.getOrThrow()
        baseDetails[listId] = detail
        recompute(listId)
        return detail
    }

    override suspend fun removeListLocally(listId: Long) {
        removeListLocallyCalls.add(listId)
    }

    override suspend fun markItemPendingDelete(
        listId: Long,
        movieId: Long,
    ) {
        markItemPendingDeleteCalls.add(listId to movieId)
        hiddenItemIds.getOrPut(listId) { mutableSetOf() }.add(movieId)
        recompute(listId)
    }

    override suspend fun clearItemPendingDelete(
        listId: Long,
        movieId: Long,
    ) {
        clearItemPendingDeleteCalls.add(listId to movieId)
        hiddenItemIds[listId]?.remove(movieId)
        recompute(listId)
    }

    override suspend fun confirmItemDelete(
        listId: Long,
        movieId: Long,
    ) {
        confirmItemDeleteCalls.add(listId to movieId)
        baseDetails[listId]?.let { detail -> baseDetails[listId] = detail.copy(items = detail.items.filter { it.id.toLong() != movieId }) }
        hiddenItemIds[listId]?.remove(movieId)
        recompute(listId)
    }

    private fun recompute(listId: Long) {
        val base = baseDetails[listId] ?: return
        val hidden = hiddenItemIds[listId].orEmpty()
        detailFlowFor(listId).value = base.copy(items = base.items.filter { it.id.toLong() !in hidden })
    }

    private fun detailFlowFor(listId: Long): MutableStateFlow<TmdbListDetail?> = detailFlows.getOrPut(listId) { MutableStateFlow(null) }
}
