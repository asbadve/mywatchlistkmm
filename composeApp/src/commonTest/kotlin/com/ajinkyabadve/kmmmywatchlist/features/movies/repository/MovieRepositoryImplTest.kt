package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
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
class MovieRepositoryImplTest {

    @Test
    fun testGetMoviesReturnsParsedPageResult() = runTest {
        val expected = MoviePageResult(
            page = 1,
            list = listOf(Movie(id = 1, title = "Popular Movie")),
            totalResults = 1,
            totalPages = 1
        )
        var requestedPath: String? = null
        var requestedQuery: String? = null
        val mockEngine = MockEngine { request ->
            requestedPath = request.url.encodedPath
            requestedQuery = request.url.encodedQuery
            respond(
                content = Json.encodeToString(MoviePageResult.serializer(), expected),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val repository = MovieRepositoryImpl(TmdbClient(mockEngine))

        val result = repository.getMovies(pageNo = 1, moveFetchType = "popular")

        assertEquals(expected, result)
        assertEquals("/3/movie/popular", requestedPath)
        assertTrue(requestedQuery.orEmpty().contains("page=1"))
    }

    @Test
    fun testGetMoviesReturnsEmptyList() = runTest {
        val expected = MoviePageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0)
        val mockEngine = MockEngine {
            respond(
                content = Json.encodeToString(MoviePageResult.serializer(), expected),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val repository = MovieRepositoryImpl(TmdbClient(mockEngine))

        val result = repository.getMovies(pageNo = 1, moveFetchType = "popular")

        assertTrue(result.list.orEmpty().isEmpty())
    }

    @Test
    fun testGetMovieDetailsReturnsParsedDetail() = runTest {
        val expected = MovieDetail(id = 42, title = "Fixture Movie", overview = "An overview")
        val mockEngine = MockEngine {
            respond(
                content = Json.encodeToString(MovieDetail.serializer(), expected),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val repository = MovieRepositoryImpl(TmdbClient(mockEngine))

        val result = repository.getMovieDetails(42)

        assertEquals(expected, result)
    }

    @Test
    fun testGetMoviesThrowsHttpExceptionsOnErrorStatus() = runTest {
        val mockEngine = MockEngine {
            respond(content = "", status = HttpStatusCode.NotFound, headers = headersOf())
        }
        val repository = MovieRepositoryImpl(TmdbClient(mockEngine))

        assertFailsWith<HttpExceptions> {
            repository.getMovies(pageNo = 1, moveFetchType = "popular")
        }
    }
}
