package com.ajinkyabadve.kmmmywatchlist.features.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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

    /** Offline changes not yet on TMDB - see [PendingSyncUiState]. Updates as the queue drains. */
    val pendingSync: StateFlow<PendingSyncUiState> =
        trackedMediaRepository
            .observeQueuedToggles(accountId, category)
            .map { toggles ->
                PendingSyncUiState(
                    notSyncedIds = toggles.filter { it.isOn && it.mediaType == mediaType.apiValue }.map { it.id }.toSet(),
                    waitingCount = toggles.size,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, PendingSyncUiState())

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }
}
