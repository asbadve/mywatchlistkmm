package com.ajinkyabadve.kmmmywatchlist.features.search.repository

import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import kotlinproject.composeapp.BuildConfig

class SearchRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : SearchRepository {
    override suspend fun searchMulti(
        query: String,
        pageNo: Int,
        includeAdult: Boolean,
    ): SearchPageResult {
        val response: HttpResponse =
            tmdbClient.client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = SEARCH_MULTI_PATH
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    // `parameters.append` percent-encodes, so spaces and punctuation in the raw
                    // user query are safe to pass straight through.
                    parameters.append(QUERY, query)
                    parameters.append(NetworkConstant.PAGE, pageNo.toString())
                    parameters.append(INCLUDE_ADULT, includeAdult.toString())
                }
            }
        return response.body()
    }

    private companion object {
        const val SEARCH_MULTI_PATH = "/3/search/multi"
        const val QUERY = "query"
        const val INCLUDE_ADULT = "include_adult"
    }
}
