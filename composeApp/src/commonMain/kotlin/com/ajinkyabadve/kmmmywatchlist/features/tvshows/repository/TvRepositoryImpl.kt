package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.network.builder.mediaHttpBuilder
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

class TvRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : TvRepository {
    override suspend fun getTvShows(pageNo: Int, moveFetchType: String): TvPageResult {
        val response: HttpResponse = tmdbClient.client.get {
            mediaHttpBuilder(moveFetchType, pageNo.toString(), TV)
        }
        return response.body()
    }

    private companion object {
        const val TV = "/3/tv/"
    }
}
