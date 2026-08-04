package com.ajinkyabadve.kmmmywatchlist.features.search.screen

import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.features.search.repository.SearchRepository
import io.ktor.utils.io.errors.IOException

class FakeSearchRepository : SearchRepository {
    var searchMultiResult: Result<SearchPageResult>? = null

    /** Every (query, page) pair the model actually requested, in order. */
    val searchMultiCalls = mutableListOf<Pair<String, Int>>()

    override suspend fun searchMulti(
        query: String,
        pageNo: Int,
    ): SearchPageResult {
        searchMultiCalls.add(query to pageNo)

        searchMultiResult?.let { result ->
            if (result.isSuccess) {
                return result.getOrThrow()
            } else {
                throw result.exceptionOrNull() ?: IOException("Fake repository error")
            }
        }

        return SearchPageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0)
    }
}
