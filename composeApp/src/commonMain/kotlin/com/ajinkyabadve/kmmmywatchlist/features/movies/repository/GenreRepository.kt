package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.ConfigurationConstants
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.utils.io.errors.IOException
import kotlinproject.composeapp.BuildConfig
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface GenreRepository {
    suspend fun getMovieGenres(): List<Genre>

    suspend fun getTvGenres(): List<Genre>
}

/**
 * Genre catalogs barely change, so this mirrors [ConfigurationRepositoryImpl]'s day-long
 * `Settings`-backed cache exactly: serve the cached list until it's a day old, then re-sync from
 * TMDB, falling back to a stale cache (rather than an empty list) on network/server error.
 */
class GenreRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
    private val settings: Settings = com.ajinkyabadve.kmmmywatchlist.createSettings(),
) : GenreRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val genreListSerializer = ListSerializer(Genre.serializer())

    override suspend fun getMovieGenres(): List<Genre> =
        getGenres(MOVIE_GENRES_PATH, GenreConstant.KEY_MOVIE_GENRES, GenreConstant.KEY_MOVIE_GENRES_TIMESTAMP)

    override suspend fun getTvGenres(): List<Genre> =
        getGenres(TV_GENRES_PATH, GenreConstant.KEY_TV_GENRES, GenreConstant.KEY_TV_GENRES_TIMESTAMP)

    private suspend fun getGenres(
        path: String,
        cacheKey: String,
        timestampKey: String,
    ): List<Genre> {
        val cachedJson = settings.getString(cacheKey, "")
        val lastFetch = settings.getLong(timestampKey, 0L)
        val now = Clock.System.now().toEpochMilliseconds()

        if (cachedJson.isNotEmpty() && ((now - lastFetch) < ConfigurationConstants.DAY_IN_MILLIS)) {
            try {
                return json.decodeFromString(genreListSerializer, cachedJson)
            } catch (e: SerializationException) {
                Napier.w(tag = TAG, throwable = e) { "Corrupted cached genre JSON, clearing cache" }
                settings.remove(cacheKey)
            } catch (e: IllegalArgumentException) {
                Napier.w(tag = TAG, throwable = e) { "Cached genre schema mismatch, clearing cache" }
                settings.remove(cacheKey)
            }
        }

        return try {
            val response: HttpResponse =
                tmdbClient.client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = NetworkConstant.HOST
                        trailingQuery = true
                        encodedPath = path
                        parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    }
                }
            val genreListResponse: GenreListResponse = response.body()
            settings.putString(cacheKey, json.encodeToString(genreListSerializer, genreListResponse.genres))
            settings.putLong(timestampKey, now)
            genreListResponse.genres
        } catch (e: ResponseException) {
            Napier.w(tag = TAG, throwable = e) { "Server error fetching genres, using fallback" }
            getFallbackGenres(cachedJson)
        } catch (e: ConnectTimeoutException) {
            Napier.w(tag = TAG, throwable = e) { "Connection timeout fetching genres, using fallback" }
            getFallbackGenres(cachedJson)
        } catch (e: HttpRequestTimeoutException) {
            Napier.w(tag = TAG, throwable = e) { "Request timeout fetching genres, using fallback" }
            getFallbackGenres(cachedJson)
        } catch (e: IOException) {
            Napier.w(tag = TAG, throwable = e) { "Network IO error fetching genres, using fallback" }
            getFallbackGenres(cachedJson)
        }
    }

    private fun getFallbackGenres(cachedJson: String): List<Genre> {
        if (cachedJson.isNotEmpty()) {
            try {
                return json.decodeFromString(genreListSerializer, cachedJson)
            } catch (e: SerializationException) {
                Napier.w(tag = TAG, throwable = e) { "Corrupted cached genre JSON, using empty fallback" }
            } catch (e: IllegalArgumentException) {
                Napier.w(tag = TAG, throwable = e) { "Cached genre schema mismatch, using empty fallback" }
            }
        }
        return emptyList()
    }

    private companion object {
        const val TAG = "GenreRepositoryImpl"
        const val MOVIE_GENRES_PATH = "/3/genre/movie/list"
        const val TV_GENRES_PATH = "/3/genre/tv/list"
    }
}

@Serializable
private data class GenreListResponse(
    val genres: List<Genre> = emptyList(),
)

private object GenreConstant {
    const val KEY_MOVIE_GENRES = "discover_movie_genres_json"
    const val KEY_MOVIE_GENRES_TIMESTAMP = "discover_movie_genres_timestamp"
    const val KEY_TV_GENRES = "discover_tv_genres_json"
    const val KEY_TV_GENRES_TIMESTAMP = "discover_tv_genres_timestamp"
}
