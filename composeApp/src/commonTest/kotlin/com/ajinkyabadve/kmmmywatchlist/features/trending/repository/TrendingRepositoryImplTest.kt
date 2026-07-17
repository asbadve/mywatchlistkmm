package com.ajinkyabadve.kmmmywatchlist.features.trending.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class TrendingRepositoryImplTest {

    @Test
    fun testGetTrendingReturnsParsedPageResult() = runTest {
        val expected = MoviePageResult(
            page = 1,
            list = listOf(Movie(id = 1, title = "Trending Movie")),
            totalResults = 1,
            totalPages = 1
        )
        var requestedPath: String? = null
        val mockEngine = MockEngine { request ->
            requestedPath = request.url.encodedPath
            respond(
                content = Json.encodeToString(MoviePageResult.serializer(), expected),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val repository = TrendingRepositoryImpl(TmdbClient(mockEngine))

        val result = repository.getTrending(timeWindow = "day", mediaType = "movie")

        assertEquals(expected, result)
        assertEquals("/3/trending/movie/day", requestedPath)
    }

    @Test
    fun testGetTrendingReturnsEmptyList() = runTest {
        val expected = MoviePageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0)
        val mockEngine = MockEngine {
            respond(
                content = Json.encodeToString(MoviePageResult.serializer(), expected),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val repository = TrendingRepositoryImpl(TmdbClient(mockEngine))

        val result = repository.getTrending(timeWindow = "day", mediaType = "movie")

        assertTrue(result.list.orEmpty().isEmpty())
    }

    @Test
    fun testGetTrendingThrowsHttpExceptionsOnErrorStatus() = runTest {
        val mockEngine = MockEngine {
            respond(content = "", status = HttpStatusCode.NotFound, headers = headersOf())
        }
        val repository = TrendingRepositoryImpl(TmdbClient(mockEngine))

        assertFailsWith<HttpExceptions> {
            repository.getTrending(timeWindow = "day", mediaType = "movie")
        }
    }
}
