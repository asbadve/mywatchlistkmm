package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository

class FakeTvRepository : TvRepository {
    override suspend fun getTvShows(pageNo: Int, moveFetchType: String): TvPageResult {
        val tvShows = listOf(
            Tv(id = 201, title = "Tv Show A", originalTitle = "Tv Show A")
        )
        return TvPageResult(
            page = 1,
            list = tvShows,
            totalResults = tvShows.size,
            totalPages = 1
        )
    }
}
