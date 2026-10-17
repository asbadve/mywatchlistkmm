package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

data class MediaActionsUiState(
    val isFavorite: Boolean = false,
    val isInWatchlist: Boolean = false,
    // Starts true: the icons render a shimmer placeholder (see MediaActionButtons) rather than a
    // guessed favorite/watchlist state until account_states actually answers - this never blocks
    // the rest of the detail screen, which loads on its own separate ScreenModel call.
    val isLoading: Boolean = true,
)
