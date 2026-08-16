package com.ajinkyabadve.kmmmywatchlist.features.search.repository

import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult

interface SearchRepository {
    /**
     * Searches movies, TV shows and people in one request via TMDB's `/3/search/multi`.
     *
     * @param query the raw user-typed text; callers are expected to have trimmed and
     *   non-blank-checked it, since TMDB rejects an empty `query` parameter.
     * @param pageNo 1-based page index.
     * @param includeAdult mirrors TMDB's `include_adult` query param - defaults to `false`.
     */
    suspend fun searchMulti(
        query: String,
        pageNo: Int,
        includeAdult: Boolean = false,
    ): SearchPageResult
}
