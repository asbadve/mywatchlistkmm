package com.ajinkyabadve.kmmmywatchlist.features.search.repository

import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SearchRepositoryImplTest {
    /**
     * A realistic `/3/search/multi` page: the three media types carry genuinely different field
     * sets (movie has `title`/`release_date`, tv has `name`/`first_air_date`, person has
     * `profile_path` and no date), captured from the live endpoint on 2026-08-04.
     */
    private val multiSearchJson =
        """
        {
          "page": 1,
          "results": [
            {
              "adult": false,
              "backdrop_path": "/backdrop.jpg",
              "id": 603,
              "title": "The Matrix",
              "original_title": "The Matrix",
              "overview": "A computer hacker learns the truth.",
              "poster_path": "/matrix.jpg",
              "media_type": "movie",
              "release_date": "1999-03-30",
              "vote_average": 8.2,
              "vote_count": 25000,
              "popularity": 120.5
            },
            {
              "adult": false,
              "backdrop_path": null,
              "id": 12345,
              "name": "The Matrix Chronicles",
              "original_name": "The Matrix Chronicles",
              "overview": "A series.",
              "poster_path": "/chronicles.jpg",
              "media_type": "tv",
              "first_air_date": "2015-06-01",
              "origin_country": ["US"],
              "vote_average": 7.1,
              "vote_count": 300,
              "popularity": 42.0
            },
            {
              "adult": false,
              "gender": 2,
              "id": 6384,
              "name": "Keanu Reeves",
              "original_name": "Keanu Reeves",
              "known_for_department": "Acting",
              "profile_path": "/keanu.jpg",
              "media_type": "person",
              "popularity": 95.3
            }
          ],
          "total_pages": 7,
          "total_results": 126
        }
        """.trimIndent()

    @Test
    fun testSearchMultiParsesAllThreeMediaTypes() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = multiSearchJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = SearchRepositoryImpl(TmdbClient(mockEngine))

            val result = repository.searchMulti(query = "matrix", pageNo = 1)
            val items = result.list.orEmpty()

            assertEquals(3, items.size)
            assertEquals(7, result.totalPages)

            val movie = items[0]
            assertEquals(SearchMediaType.MOVIE, movie.mediaType)
            assertEquals("The Matrix", movie.displayTitle)
            assertEquals("/matrix.jpg", movie.imagePath)
            assertEquals("1999", movie.releaseYear)

            val tv = items[1]
            assertEquals(SearchMediaType.TV, tv.mediaType)
            // TV shows title themselves with `name`, not `title`.
            assertEquals("The Matrix Chronicles", tv.displayTitle)
            assertEquals("2015", tv.releaseYear)

            val person = items[2]
            assertEquals(SearchMediaType.PERSON, person.mediaType)
            assertEquals("Keanu Reeves", person.displayTitle)
            // People have no poster_path - the headshot comes from profile_path instead.
            assertEquals("/keanu.jpg", person.imagePath)
            assertEquals(null, person.releaseYear)
        }

    @Test
    fun testSearchMultiSendsQueryAndPageParameters() =
        runTest {
            var requestedPath: String? = null
            var requestedQuery: String? = null
            val mockEngine =
                MockEngine { request ->
                    requestedPath = request.url.encodedPath
                    requestedQuery = request.url.encodedQuery
                    respond(
                        content = multiSearchJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = SearchRepositoryImpl(TmdbClient(mockEngine))

            repository.searchMulti(query = "the matrix", pageNo = 2)

            assertEquals("/3/search/multi", requestedPath)
            assertTrue(requestedQuery.orEmpty().contains("page=2"), "missing page in $requestedQuery")
            // The space must be percent-encoded rather than sent raw.
            assertTrue(
                requestedQuery.orEmpty().contains("query=the%20matrix") ||
                    requestedQuery.orEmpty().contains("query=the+matrix"),
                "query not encoded in $requestedQuery",
            )
            assertTrue(requestedQuery.orEmpty().contains("include_adult=false"))
        }

    @Test
    fun testSearchMultiHandlesEmptyResults() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"page":1,"results":[],"total_pages":0,"total_results":0}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = SearchRepositoryImpl(TmdbClient(mockEngine))

            val result = repository.searchMulti(query = "zzzzzzzz", pageNo = 1)

            assertTrue(result.list.orEmpty().isEmpty())
        }

    @Test
    fun testSearchMultiThrowsHttpExceptionsOnErrorStatus() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(content = "", status = HttpStatusCode.NotFound, headers = headersOf())
                }
            val repository = SearchRepositoryImpl(TmdbClient(mockEngine))

            assertFailsWith<HttpExceptions> {
                repository.searchMulti(query = "matrix", pageNo = 1)
            }
        }
}
