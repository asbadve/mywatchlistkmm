package com.ajinkyabadve.kmmmywatchlist.features.search.repository

import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult

interface SearchRepository {
    /**
     * Searches movies, TV shows and people in one request via TMDB's `/3/search/multi`.
     *
     * @param query the raw user-typed text; callers are expected to have trimmed and
     *   non-blank-checked it, since TMDB rejects an empty `query` parameter.
     * @param pageNo 1-based page index.
     */
    suspend fun searchMulti(
        query: String,
        pageNo: Int,
    ): SearchPageResult
}
