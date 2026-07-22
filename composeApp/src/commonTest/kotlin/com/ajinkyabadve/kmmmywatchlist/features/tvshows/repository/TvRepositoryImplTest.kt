package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TvRepositoryImplTest {
    @Test
    fun testGetTvShowsReturnsParsedPageResult() =
        runTest {
            val expected =
                TvPageResult(
                    page = 1,
                    list = listOf(Tv(id = 1, title = "Popular Show")),
                    totalResults = 1,
                    totalPages = 1,
                )
            var requestedPath: String? = null
            val mockEngine =
                MockEngine { request ->
                    requestedPath = request.url.encodedPath
                    respond(
                        content = Json.encodeToString(TvPageResult.serializer(), expected),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = TvRepositoryImpl(TmdbClient(mockEngine))

            val result = repository.getTvShows(pageNo = 1, moveFetchType = "popular")

            assertEquals(expected, result)
            assertEquals("/3/tv/popular", requestedPath)
        }

    @Test
    fun testGetTvShowsReturnsEmptyList() =
        runTest {
            val expected = TvPageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0)
            val mockEngine =
                MockEngine {
                    respond(
                        content = Json.encodeToString(TvPageResult.serializer(), expected),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = TvRepositoryImpl(TmdbClient(mockEngine))

            val result = repository.getTvShows(pageNo = 1, moveFetchType = "popular")

            assertTrue(result.list.orEmpty().isEmpty())
        }

    @Test
    fun testGetTvDetailsReturnsParsedDetail() =
        runTest {
            val expected = TvDetail(id = 7, title = "Fixture Show", overview = "An overview")
            var requestedPath: String? = null
            val mockEngine =
                MockEngine { request ->
                    requestedPath = request.url.encodedPath
                    respond(
                        content = Json.encodeToString(TvDetail.serializer(), expected),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = TvRepositoryImpl(TmdbClient(mockEngine))

            val result = repository.getTvDetails(7)

            assertEquals(expected, result)
            assertEquals("/3/tv/7", requestedPath)
        }

    @Test
    fun testGetSeasonDetailsReturnsParsedSeason() =
        runTest {
            val expected = TvSeasonDetail(id = 7, seasonNumber = 2, name = "Season 2")
            var requestedPath: String? = null
            val mockEngine =
                MockEngine { request ->
                    requestedPath = request.url.encodedPath
                    respond(
                        content = Json.encodeToString(TvSeasonDetail.serializer(), expected),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = TvRepositoryImpl(TmdbClient(mockEngine))

            val result = repository.getSeasonDetails(tvId = 7, seasonNumber = 2)

            assertEquals(expected, result)
            assertEquals("/3/tv/7/season/2", requestedPath)
        }

    @Test
    fun testGetTvDetailsThrowsHttpExceptionsOnErrorStatus() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(content = "", status = HttpStatusCode.NotFound, headers = headersOf())
                }
            val repository = TvRepositoryImpl(TmdbClient(mockEngine))

            assertFailsWith<HttpExceptions> {
                repository.getTvDetails(7)
            }
        }
}
