package com.ajinkyabadve.kmmmywatchlist.features.trending.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult

interface TrendingRepository {
    suspend fun getTrending(
        timeWindow: String,
        mediaType: String,
    ): MoviePageResult
}
