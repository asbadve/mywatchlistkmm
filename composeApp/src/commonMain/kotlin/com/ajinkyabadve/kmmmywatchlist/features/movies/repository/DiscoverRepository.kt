package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import kotlinproject.composeapp.BuildConfig

interface DiscoverRepository {
    suspend fun getDiscoverMovies(
        pageNo: Int,
        filters: DiscoverFilters,
        includeAdult: Boolean,
    ): MoviePageResult

    suspend fun getDiscoverTvShows(
        pageNo: Int,
        filters: DiscoverFilters,
        includeAdult: Boolean,
    ): TvPageResult
}

/**
 * `/3/discover/movie` and `/3/discover/tv` take many more query params than the rest of
 * `MovieRepository`/`TvRepository`'s endpoints, so this stays a dedicated repository rather than
 * bolted onto either - both return the existing `MoviePageResult`/`TvPageResult` shapes, no new
 * result models needed.
 */
class DiscoverRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : DiscoverRepository {
    override suspend fun getDiscoverMovies(
        pageNo: Int,
        filters: DiscoverFilters,
        includeAdult: Boolean,
    ): MoviePageResult {
        val response: HttpResponse =
            tmdbClient.client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = DISCOVER_MOVIE_PATH
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.PAGE, pageNo.toString())
                    parameters.append(SORT_BY, filters.sortBy)
                    parameters.append(INCLUDE_ADULT, includeAdult.toString())
                    if (filters.genreIds.isNotEmpty()) parameters.append(WITH_GENRES, filters.genreIds.joinToString(","))
                    if (filters.keywords.isNotEmpty()) {
                        parameters.append(
                            WITH_KEYWORDS,
                            filters.keywords.joinToString(",") { it.id.toString() },
                        )
                    }
                    filters.year?.let { parameters.append(PRIMARY_RELEASE_YEAR, it.toString()) }
                }
            }
        return response.body()
    }

    override suspend fun getDiscoverTvShows(
        pageNo: Int,
        filters: DiscoverFilters,
        includeAdult: Boolean,
    ): TvPageResult {
        val response: HttpResponse =
            tmdbClient.client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = DISCOVER_TV_PATH
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.PAGE, pageNo.toString())
                    parameters.append(SORT_BY, filters.sortBy)
                    parameters.append(INCLUDE_ADULT, includeAdult.toString())
                    if (filters.genreIds.isNotEmpty()) parameters.append(WITH_GENRES, filters.genreIds.joinToString(","))
                    if (filters.keywords.isNotEmpty()) {
                        parameters.append(
                            WITH_KEYWORDS,
                            filters.keywords.joinToString(",") { it.id.toString() },
                        )
                    }
                    filters.year?.let { parameters.append(FIRST_AIR_DATE_YEAR, it.toString()) }
                }
            }
        return response.body()
    }

    private companion object {
        const val DISCOVER_MOVIE_PATH = "/3/discover/movie"
        const val DISCOVER_TV_PATH = "/3/discover/tv"
        const val SORT_BY = "sort_by"
        const val INCLUDE_ADULT = "include_adult"
        const val WITH_GENRES = "with_genres"
        const val WITH_KEYWORDS = "with_keywords"
        const val PRIMARY_RELEASE_YEAR = "primary_release_year"
        const val FIRST_AIR_DATE_YEAR = "first_air_date_year"
    }
}
