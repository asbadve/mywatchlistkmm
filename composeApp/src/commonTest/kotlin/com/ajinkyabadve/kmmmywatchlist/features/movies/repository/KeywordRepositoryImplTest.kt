package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeywordRepositoryImplTest {
    @Test
    fun testSearchKeywordsReturnsMappedResults() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = """{"page":1,"results":[{"id":9715,"name":"superhero"}],"total_results":1,"total_pages":1}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = KeywordRepositoryImpl(TmdbClient(mockEngine))

            val results = repository.searchKeywords("super")

            assertEquals(1, results.size)
            assertEquals(9715, results.first().id)
            assertEquals("superhero", results.first().name)
        }

    @Test
    fun testBlankQueryReturnsEmptyWithoutNetworkCall() =
        runTest {
            var networkCalls = 0
            val mockEngine =
                MockEngine {
                    networkCalls++
                    respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf())
                }
            val repository = KeywordRepositoryImpl(TmdbClient(mockEngine))

            val results = repository.searchKeywords("  ")

            assertTrue(results.isEmpty())
            assertEquals(0, networkCalls)
        }
}
