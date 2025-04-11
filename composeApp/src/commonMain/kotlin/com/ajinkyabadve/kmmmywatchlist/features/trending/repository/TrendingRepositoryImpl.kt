package com.ajinkyabadve.kmmmywatchlist.features.trending.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.network.builder.trendingMediaHttpBuilder
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

class TrendingRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : TrendingRepository {
    override suspend fun getTrending(
        timeWindow: String,
        mediaType: String,
    ): MoviePageResult {
        val response: HttpResponse =
            tmdbClient.client.get {
                trendingMediaHttpBuilder(
                    timeWindow = timeWindow,
                    mediaType = mediaType,
                    path = TRENDING,
                )
            }
        return response.body()
    }

    private companion object {
        const val TRENDING = "/3/trending"
    }
}
