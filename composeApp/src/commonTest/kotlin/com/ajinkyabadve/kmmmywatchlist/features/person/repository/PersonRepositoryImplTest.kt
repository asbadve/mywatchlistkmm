package com.ajinkyabadve.kmmmywatchlist.features.person.repository

import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult
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

class PersonRepositoryImplTest {
    @Test
    fun testGetPopularPeopleReturnsParsedPageResult() =
        runTest {
            val expected =
                PersonPageResult(
                    page = 1,
                    list = listOf(Person(id = 1, name = "Popular Person")),
                    totalResults = 1,
                    totalPages = 1,
                )
            var requestedPath: String? = null
            val mockEngine =
                MockEngine { request ->
                    requestedPath = request.url.encodedPath
                    respond(
                        content = Json.encodeToString(PersonPageResult.serializer(), expected),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = PersonRepositoryImpl(TmdbClient(mockEngine))

            val result = repository.getPopularPeople(pageNo = 1)

            assertEquals(expected, result)
            assertEquals("/3/person/popular", requestedPath)
        }

    @Test
    fun testGetPopularPeopleReturnsEmptyList() =
        runTest {
            val expected = PersonPageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0)
            val mockEngine =
                MockEngine {
                    respond(
                        content = Json.encodeToString(PersonPageResult.serializer(), expected),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val repository = PersonRepositoryImpl(TmdbClient(mockEngine))

            val result = repository.getPopularPeople(pageNo = 1)

            assertTrue(result.list.orEmpty().isEmpty())
        }

    @Test
    fun testGetPopularPeopleThrowsHttpExceptionsOnErrorStatus() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(content = "", status = HttpStatusCode.NotFound, headers = headersOf())
                }
            val repository = PersonRepositoryImpl(TmdbClient(mockEngine))

            assertFailsWith<HttpExceptions> {
                repository.getPopularPeople(pageNo = 1)
            }
        }
}
