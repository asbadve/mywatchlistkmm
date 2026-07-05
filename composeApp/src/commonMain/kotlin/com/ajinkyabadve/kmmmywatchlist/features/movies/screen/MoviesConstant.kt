package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

object MoviesConstant {
    @Deprecated("Use ImageConfigResolver instead to resolve dynamic resolutions based on density")
    const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185/"
    const val NOW_PLAYING_MOVIES = "Now Playing"
    const val UPCOMING_MOVIES = "Upcoming"
    const val POPULAR_MOVIES = "Popular"
    const val TOP_RATED_MOVIES = "Top Rated"

    const val NOW_PLAYING_API_PATH = "now_playing"
    const val UPCOMING_API_PATH = "upcoming"
    const val POPULAR_API_PATH = "popular"
    const val TOP_RATED_API_PATH = "top_rated"

    const val SIXTH_INDEX = 6
    const val THIRD_INDEX = 3

    val movieChipList =
        listOf(
            NOW_PLAYING_MOVIES,
            UPCOMING_MOVIES,
            POPULAR_MOVIES,
            TOP_RATED_MOVIES,
        )
}

sealed interface MovieFilterState {
    data class Success(
        val selectedChip: Int = 0,
        val chipItemList: List<String> = listOf(),
    ) : MovieFilterState
}
