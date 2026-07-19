package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.EpisodeDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.network.builder.mediaHttpBuilder
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import kotlinproject.composeapp.BuildConfig

class TvRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : TvRepository {
    override suspend fun getTvShows(pageNo: Int, moveFetchType: String): TvPageResult {
        val response: HttpResponse = tmdbClient.client.get {
            mediaHttpBuilder(moveFetchType, pageNo.toString(), TV)
        }
        return response.body()
    }

    override suspend fun getTvDetails(tvId: Long): TvDetail {
        val response: HttpResponse = tmdbClient.client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = NetworkConstant.HOST
                trailingQuery = true
                encodedPath = "$TV$tvId"
                parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                parameters.append("append_to_response", APPEND_TO_RESPONSE)
            }
        }
        return response.body()
    }

    override suspend fun getSeasonDetails(tvId: Long, seasonNumber: Int): TvSeasonDetail {
        val response: HttpResponse = tmdbClient.client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = NetworkConstant.HOST
                trailingQuery = true
                encodedPath = "$TV$tvId/season/$seasonNumber"
                parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                parameters.append("append_to_response", APPEND_TO_RESPONSE)
            }
        }
        return response.body()
    }

    override suspend fun getEpisodeDetails(tvId: Long, seasonNumber: Int, episodeNumber: Int): EpisodeDetail {
        val response: HttpResponse = tmdbClient.client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = NetworkConstant.HOST
                trailingQuery = true
                encodedPath = "$TV$tvId/season/$seasonNumber/episode/$episodeNumber"
                parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                parameters.append("append_to_response", EPISODE_APPEND_TO_RESPONSE)
            }
        }
        return response.body()
    }

    private companion object {
        const val TV = "/3/tv/"
        const val APPEND_TO_RESPONSE = "content_ratings,credits,external_ids,images,keywords,recommendations,videos,similar"
        const val EPISODE_APPEND_TO_RESPONSE = "credits,external_ids,images,translations,videos"
    }
}
