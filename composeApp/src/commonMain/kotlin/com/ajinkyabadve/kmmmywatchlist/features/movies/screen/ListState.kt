package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

enum class ListState {
    IDLE,
    LOADING,
    PAGINATING,
    ERROR,
    NETWORK_ERROR,
    PAGINATION_EXHAUST,
}
