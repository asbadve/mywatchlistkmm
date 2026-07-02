package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.network.builder.mediaHttpBuilder
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import kotlinproject.composeapp.BuildConfig

class MovieRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : MovieRepository {

    override suspend fun getMovies(pageNo: Int, moveFetchType: String): MoviePageResult {
        val response: HttpResponse = tmdbClient.client.get {
            mediaHttpBuilder(moveFetchType, pageNo.toString(), MOVIE)
        }
        return response.body()
    }

    override suspend fun getMovieDetails(movieId: Long): MovieDetail {
        val response: HttpResponse = tmdbClient.client.get {
            url {
                protocol = URLProtocol.HTTPS
                host = NetworkConstant.HOST
                trailingQuery = true
                encodedPath = "$MOVIE$movieId"
                parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                parameters.append("append_to_response", "videos,images,credits,keywords,alternative_titles,changes,external_ids,release_dates,translations,recommendations,similar,reviews")
            }
        }
        return response.body()
    }

    private companion object {
        const val MOVIE = "/3/movie/"
    }
}
