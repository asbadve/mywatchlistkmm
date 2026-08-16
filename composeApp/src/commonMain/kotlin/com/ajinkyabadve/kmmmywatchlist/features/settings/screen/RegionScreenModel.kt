package com.ajinkyabadve.kmmmywatchlist.features.settings.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.settings.model.WatchProviderRegion
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

sealed interface RegionLoadState {
    data object Loading : RegionLoadState

    data object Loaded : RegionLoadState
}

/**
 * Backs the region picker dialog - loads the regions TMDB has watch-provider data for
 * ([RegionRepository.getAvailableRegions] never throws, it falls back to a cached/empty list on
 * network error) and filters them by the search query.
 *
 * Deliberately does not persist a pick itself: the same picker serves both the "selected region"
 * and "default fallback region" settings on [com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AccountScreen] -
 * which repository setter to call is the caller's decision, not this model's.
 */
class RegionScreenModel(
    private val regionRepository: RegionRepository = RegionRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private val allRegions = mutableListOf<WatchProviderRegion>()
    internal val filteredRegions = mutableStateListOf<WatchProviderRegion>()

    var loadState by mutableStateOf<RegionLoadState>(RegionLoadState.Loading)
        private set
    var searchQuery by mutableStateOf("")
        private set

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.Main) {
            loadState = RegionLoadState.Loading
            allRegions.clear()
            allRegions.addAll(regionRepository.getAvailableRegions().sortedBy { it.englishName })
            applyFilter()
            loadState = RegionLoadState.Loaded
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        applyFilter()
    }

    private fun applyFilter() {
        filteredRegions.clear()
        filteredRegions.addAll(
            if (searchQuery.isBlank()) {
                allRegions
            } else {
                allRegions.filter {
                    it.englishName.contains(searchQuery, ignoreCase = true) || it.iso3166.contains(searchQuery, ignoreCase = true)
                }
            },
        )
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }
}
