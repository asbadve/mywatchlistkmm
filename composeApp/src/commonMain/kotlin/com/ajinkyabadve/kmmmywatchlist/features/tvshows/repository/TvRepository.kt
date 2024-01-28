package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult


interface TvRepository {
    suspend fun getTvShows(pageNo: Int, moveFetchType: String): TvPageResult
}
