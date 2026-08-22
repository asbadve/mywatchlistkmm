package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.PagingData
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-in for [TrackedMediaRepository] - screen model tests need this so they never
 * touch [com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider]'s real platform driver (which
 * would mean real disk I/O from a unit test). Repository-level behavior itself (upsert/delete
 * reconciliation, poll-state preservation) is covered against a real in-memory SQLite database in
 * `desktopTest`'s `TrackedMediaRepositoryImplTest` instead - this fake is deliberately dumb.
 */
class FakeTrackedMediaRepository : TrackedMediaRepository {
    val markPendingDeleteCalls = mutableListOf<Triple<Int, String, AccountMediaCategory>>()
    val clearPendingDeleteCalls = mutableListOf<Triple<Int, String, AccountMediaCategory>>()
    val confirmDeleteCalls = mutableListOf<Triple<Int, String, AccountMediaCategory>>()

    private val pagedFlows = mutableMapOf<Pair<AccountMediaCategory, SearchMediaType>, MutableStateFlow<List<SearchResultItem>>>()

    /** Seeds what [pagedFlow] emits for (category, mediaType) - dumb, no RemoteMediator/network
     *  simulation. `TrackedMediaRemoteMediator`'s own fetch/upsert behavior is covered against a
     *  real in-memory database in `desktopTest` instead. */
    fun seedPaged(
        category: AccountMediaCategory,
        mediaType: SearchMediaType,
        items: List<SearchResultItem>,
    ) {
        pagedFlowFor(category, mediaType).value = items
    }

    val pagedFlowCalls = mutableListOf<PagedFlowCall>()

    data class PagedFlowCall(
        val category: AccountMediaCategory,
        val mediaType: SearchMediaType,
        val accountId: Long,
        val sessionId: String,
    )

    override fun pagedFlow(
        category: AccountMediaCategory,
        mediaType: SearchMediaType,
        accountId: Long,
        sessionId: String,
    ): Flow<PagingData<SearchResultItem>> {
        pagedFlowCalls.add(PagedFlowCall(category, mediaType, accountId, sessionId))
        return pagedFlowFor(category, mediaType).map { PagingData.from(it) }
    }

    private fun pagedFlowFor(
        category: AccountMediaCategory,
        mediaType: SearchMediaType,
    ): MutableStateFlow<List<SearchResultItem>> = pagedFlows.getOrPut(category to mediaType) { MutableStateFlow(emptyList()) }

    override suspend fun updatePollState(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
        nextEpisodeAirDate: String?,
        creditIds: String?,
    ) {
        // Not needed by any current screen model test - the poll-state contract itself is covered
        // against a real database in TrackedMediaRepositoryImplTest.
    }

    override suspend fun markPendingDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    ) {
        markPendingDeleteCalls.add(Triple(id, mediaType, category))
    }

    override suspend fun clearPendingDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    ) {
        clearPendingDeleteCalls.add(Triple(id, mediaType, category))
    }

    override suspend fun confirmDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    ) {
        confirmDeleteCalls.add(Triple(id, mediaType, category))
    }
}
