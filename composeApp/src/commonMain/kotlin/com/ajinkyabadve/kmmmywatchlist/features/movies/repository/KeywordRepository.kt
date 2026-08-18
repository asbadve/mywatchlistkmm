package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Keyword
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import kotlinproject.composeapp.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface KeywordRepository {
    suspend fun searchKeywords(query: String): List<Keyword>
}

/** Query-driven autocomplete against `/3/search/keyword` - not cached, unlike [GenreRepository]. */
class KeywordRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : KeywordRepository {
    override suspend fun searchKeywords(query: String): List<Keyword> {
        if (query.isBlank()) return emptyList()
        val response: HttpResponse =
            tmdbClient.client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = KEYWORD_SEARCH_PATH
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(QUERY_PARAM, query)
                }
            }
        val result: KeywordPageResult = response.body()
        return result.list.orEmpty()
    }

    private companion object {
        const val KEYWORD_SEARCH_PATH = "/3/search/keyword"
        const val QUERY_PARAM = "query"
    }
}

/** `/3/search/keyword`'s paged response shape - distinct from the append_to_response `KeywordResponse`. */
@Serializable
private data class KeywordPageResult(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val list: List<Keyword>? = null,
    @SerialName("total_results") val totalResults: Int? = null,
    @SerialName("total_pages") val totalPages: Int? = null,
)
