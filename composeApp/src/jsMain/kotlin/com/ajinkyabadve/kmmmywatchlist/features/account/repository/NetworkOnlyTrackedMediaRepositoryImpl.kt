package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import kotlinx.coroutines.flow.Flow

private object NetworkOnlyTrackedMediaConstant {
    // TMDB's page size is fixed (not configurable via the API).
    const val PAGE_SIZE = 20
}

/**
 * Web-only stand-in for [SqliteTrackedMediaRepositoryImpl] - see `createTrackedMediaRepository`'s
 * kdoc (in commonMain's TrackedMediaRepository.kt) for why the web target skips SQLite entirely
 * rather than fighting sql.js/WebWorkerDriver's build and bundling cost for a platform with no
 * offline story to begin with. [pagedFlow] fetches TMDB directly, one page at a time, with no
 * local mirror behind it - so there's nothing for [markPendingDelete]/[clearPendingDelete]/
 * [confirmDelete]/[updatePollState] to update either; they're no-ops here.
 */
internal class NetworkOnlyTrackedMediaRepositoryImpl(
    private val accountMediaRepository: AccountMediaRepository,
) : TrackedMediaRepository {
    override fun pagedFlow(
        category: AccountMediaCategory,
        mediaType: SearchMediaType,
        accountId: Long,
        sessionId: String,
    ): Flow<PagingData<SearchResultItem>> =
        // initialLoadSize defaults to pageSize * 3 - see SqliteTrackedMediaRepositoryImpl's
        // identical comment for why this is pinned to one page instead.
        Pager(
            PagingConfig(
                pageSize = NetworkOnlyTrackedMediaConstant.PAGE_SIZE,
                initialLoadSize = NetworkOnlyTrackedMediaConstant.PAGE_SIZE,
            ),
        ) {
            NetworkPagingSource { page ->
                val response = fetchPage(category, mediaType, accountId, sessionId, page)
                response.list.orEmpty() to response.totalPages
            }
        }.flow

    override suspend fun updatePollState(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
        nextEpisodeAirDate: String?,
        creditIds: String?,
    ) = Unit

    override suspend fun markPendingDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    ) = Unit

    override suspend fun clearPendingDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    ) = Unit

    override suspend fun confirmDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    ) = Unit

    private suspend fun fetchPage(
        category: AccountMediaCategory,
        mediaType: SearchMediaType,
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult =
        when (category) {
            AccountMediaCategory.FAVORITES ->
                when (mediaType) {
                    SearchMediaType.MOVIE -> accountMediaRepository.getFavoriteMovies(accountId, sessionId, page)
                    SearchMediaType.TV -> accountMediaRepository.getFavoriteTv(accountId, sessionId, page)
                    SearchMediaType.PERSON -> error("Favorites has no person media type")
                }

            AccountMediaCategory.WATCHLIST ->
                when (mediaType) {
                    SearchMediaType.MOVIE -> accountMediaRepository.getWatchlistMovies(accountId, sessionId, page)
                    SearchMediaType.TV -> accountMediaRepository.getWatchlistTv(accountId, sessionId, page)
                    SearchMediaType.PERSON -> error("Watchlist has no person media type")
                }
        }
}
