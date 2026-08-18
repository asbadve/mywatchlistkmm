package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiscoverRepositoryImplTest {
    private fun MockRequestHandleScope.emptyMoviePageResponse() =
        respond(
            content = """{"page":1,"results":[],"total_results":0,"total_pages":1}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )

    @Test
    fun testDiscoverMoviesSendsGenreYearAndSortByParams() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine {
                    capturedRequest = it
                    emptyMoviePageResponse()
                }
            val repository = DiscoverRepositoryImpl(TmdbClient(mockEngine))
            val filters = DiscoverFilters(genreIds = setOf(28, 12), year = 2020, sortBy = "vote_average.desc")

            repository.getDiscoverMovies(pageNo = 2, filters = filters, includeAdult = false)

            val url = capturedRequest?.url
            assertEquals("/3/discover/movie", url?.encodedPath)
            assertEquals("28,12", url?.parameters?.get("with_genres"))
            assertEquals("2020", url?.parameters?.get("primary_release_year"))
            assertEquals("vote_average.desc", url?.parameters?.get("sort_by"))
            assertEquals("2", url?.parameters?.get("page"))
            assertEquals("false", url?.parameters?.get("include_adult"))
        }

    @Test
    fun testDiscoverMoviesOmitsGenreAndYearParamsWhenUnset() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine {
                    capturedRequest = it
                    emptyMoviePageResponse()
                }
            val repository = DiscoverRepositoryImpl(TmdbClient(mockEngine))

            repository.getDiscoverMovies(pageNo = 1, filters = DiscoverFilters(), includeAdult = false)

            val url = capturedRequest?.url
            assertNull(url?.parameters?.get("with_genres"))
            assertNull(url?.parameters?.get("primary_release_year"))
        }

    @Test
    fun testDiscoverTvShowsSendsFirstAirDateYearInsteadOfPrimaryReleaseYear() =
        runTest {
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine {
                    capturedRequest = it
                    respond(
                        content = """{"page":1,"results":[],"total_results":0,"total_pages":1}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = DiscoverRepositoryImpl(TmdbClient(mockEngine))

            repository.getDiscoverTvShows(pageNo = 1, filters = DiscoverFilters(year = 2019), includeAdult = false)

            val url = capturedRequest?.url
            assertEquals("/3/discover/tv", url?.encodedPath)
            assertEquals("2019", url?.parameters?.get("first_air_date_year"))
            assertNull(url?.parameters?.get("primary_release_year"))
        }
}
