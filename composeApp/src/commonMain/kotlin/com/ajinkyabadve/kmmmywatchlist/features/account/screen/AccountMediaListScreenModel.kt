package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow

enum class AccountMediaCategory {
    FAVORITES,
    WATCHLIST,
}

/**
 * Paging3-backed - one (category, mediaType) slice of the signed-in user's favorites/watchlist,
 * mirroring `AccountFavoritesWatchlistTab`'s Movie/TV chip. Unlike the pre-Paging3 version of this
 * class, pagination itself (page/canPaginate bookkeeping, network fetch, local upsert) now lives in
 * [TrackedMediaRepository.pagedFlow]/`TrackedMediaRemoteMediator` - the repository, not this
 * ScreenModel, owns "when to fetch the next page". [mediaType] must be [SearchMediaType.MOVIE] or
 * [SearchMediaType.TV] - the account-media endpoints have no concept of a person result, so callers
 * (the Favorites/Watchlist chip rows) never offer that option.
 */
class AccountMediaListScreenModel(
    category: AccountMediaCategory,
    mediaType: SearchMediaType,
    accountId: Long,
    sessionId: String,
    trackedMediaRepository: TrackedMediaRepository = TrackedMediaRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    /** Collect via `collectAsLazyPagingItems()` - `.cachedIn` survives recomposition/tab switches. */
    val pagedItems: Flow<PagingData<SearchResultItem>> =
        trackedMediaRepository.pagedFlow(category, mediaType, accountId, sessionId).cachedIn(viewModelScope)

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }
}
