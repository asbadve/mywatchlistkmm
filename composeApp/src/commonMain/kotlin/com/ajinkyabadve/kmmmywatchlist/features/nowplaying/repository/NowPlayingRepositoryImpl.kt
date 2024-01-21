package com.ajinkyabadve.kmmmywatchlist.features.nowplaying.repository

import MyWatchList.composeApp.BuildConfig.TMDB_API_KEY
import com.ajinkyabadve.kmmmywatchlist.features.nowplaying.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.network.TmdbClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath

class NowPlayingRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : NowPlayingRepository {

    override suspend fun getNowPlayingMovies(pageNo: Int, moveFetchType: String): MoviePageResult {
        val response: HttpResponse = tmdbClient.client.get {
            nowPlaying(moveFetchType, pageNo.toString())
        }
        return response.body()
    }

    private fun HttpRequestBuilder.nowPlaying(path: String, pageNumber: String) {
        url {
            protocol = URLProtocol.HTTPS
            host = HOST
            encodedPath = "$MOVIE$path"
            trailingQuery = true
            parameters.append(API_KEY, TMDB_API_KEY)
            parameters.append(PAGE, pageNumber)
        }
    }

    companion object {
        private const val HOST = "api.themoviedb.org"
        private const val API_KEY = "api_key"
        private const val PAGE = "page"
        private const val MOVIE = "/3/movie/"
    }
}
