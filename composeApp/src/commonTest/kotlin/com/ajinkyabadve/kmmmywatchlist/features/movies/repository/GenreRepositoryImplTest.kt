package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.ConfigurationConstants
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeSettings
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class GenreRepositoryImplTest {
    private val fixtureGenres = listOf(Genre(id = 28, name = "Action"), Genre(id = 35, name = "Comedy"))
    private val genreListSerializer = ListSerializer(Genre.serializer())

    private fun genresJson() = Json.encodeToString(genreListSerializer, fixtureGenres)

    @Test
    fun testReturnsCachedMovieGenresWithoutNetworkCallWhenFresh() =
        runTest {
            val settings =
                FakeSettings().apply {
                    putString(MOVIE_GENRES_KEY, genresJson())
                    putLong(MOVIE_GENRES_TIMESTAMP_KEY, Clock.System.now().toEpochMilliseconds())
                }
            var networkCalls = 0
            val mockEngine =
                MockEngine {
                    networkCalls++
                    respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf())
                }
            val repository = GenreRepositoryImpl(TmdbClient(mockEngine), settings)

            val result = repository.getMovieGenres()

            assertEquals(fixtureGenres, result)
            assertEquals(0, networkCalls)
        }

    @Test
    fun testRefetchesMovieGenresWhenCacheIsStale() =
        runTest {
            val settings =
                FakeSettings().apply {
                    putString(MOVIE_GENRES_KEY, genresJson())
                    putLong(
                        MOVIE_GENRES_TIMESTAMP_KEY,
                        Clock.System.now().toEpochMilliseconds() - 2 * ConfigurationConstants.DAY_IN_MILLIS,
                    )
                }
            var networkCalls = 0
            val mockEngine =
                MockEngine {
                    networkCalls++
                    respond(
                        content = """{"genres":[{"id":28,"name":"Action"},{"id":35,"name":"Comedy"}]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = GenreRepositoryImpl(TmdbClient(mockEngine), settings)

            val result = repository.getMovieGenres()

            assertEquals(1, networkCalls)
            assertEquals(fixtureGenres, result)
        }

    @Test
    fun testFallsBackToStaleCacheOnNetworkErrorWhenCacheExists() =
        runTest {
            val settings =
                FakeSettings().apply {
                    putString(MOVIE_GENRES_KEY, genresJson())
                    putLong(
                        MOVIE_GENRES_TIMESTAMP_KEY,
                        Clock.System.now().toEpochMilliseconds() - 2 * ConfigurationConstants.DAY_IN_MILLIS,
                    )
                }
            val mockEngine = MockEngine { respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf()) }
            val repository = GenreRepositoryImpl(TmdbClient(mockEngine), settings)

            val result = repository.getMovieGenres()

            assertEquals(fixtureGenres, result)
        }

    @Test
    fun testTvGenresCachedIndependentlyFromMovieGenres() =
        runTest {
            val tvGenres = listOf(Genre(id = 10759, name = "Action & Adventure"))
            val settings =
                FakeSettings().apply {
                    putString(TV_GENRES_KEY, Json.encodeToString(genreListSerializer, tvGenres))
                    putLong(TV_GENRES_TIMESTAMP_KEY, Clock.System.now().toEpochMilliseconds())
                }
            var networkCalls = 0
            val mockEngine =
                MockEngine {
                    networkCalls++
                    respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf())
                }
            val repository = GenreRepositoryImpl(TmdbClient(mockEngine), settings)

            val result = repository.getTvGenres()

            assertEquals(tvGenres, result)
            assertEquals(0, networkCalls)
        }

    private companion object {
        const val MOVIE_GENRES_KEY = "discover_movie_genres_json"
        const val MOVIE_GENRES_TIMESTAMP_KEY = "discover_movie_genres_timestamp"
        const val TV_GENRES_KEY = "discover_tv_genres_json"
        const val TV_GENRES_TIMESTAMP_KEY = "discover_tv_genres_timestamp"
    }
}
