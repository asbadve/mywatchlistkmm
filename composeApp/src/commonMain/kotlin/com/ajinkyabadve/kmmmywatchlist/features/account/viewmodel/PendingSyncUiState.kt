package com.ajinkyabadve.kmmmywatchlist.features.account.viewmodel

/**
 * What the Favorites/Watchlist grid shows about changes made offline that haven't reached TMDB.
 * [notSyncedIds] are the titles in this grid (this list and media type) added offline - each
 * poster gets a "not synced" marker. [waitingCount] is every queued change in this list, both
 * media types, removals included - a removed title is already gone from the grid, so the count
 * line above it is the only place it shows.
 */
data class PendingSyncUiState(
    val notSyncedIds: Set<Int> = emptySet(),
    val waitingCount: Int = 0,
)
