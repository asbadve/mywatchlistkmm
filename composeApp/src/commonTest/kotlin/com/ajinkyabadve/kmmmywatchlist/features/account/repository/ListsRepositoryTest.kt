package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private object ListsRepositoryTestConstant {
    const val ACCOUNT_ID = 100L
    const val SESSION_ID = "session_abc"
    const val LIST_ID = 5861L
    const val MOVIE_ID = 634649L
}

class ListsRepositoryTest {
    @Test
    fun testGetListsParsesResults() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/3/account/${ListsRepositoryTestConstant.ACCOUNT_ID}/lists", request.url.encodedPath)
                    respond(
                        content =
                            """
                            {"page":1,"results":[{"id":1,"name":"The Marvel Universe","description":"","item_count":59,"poster_path":null}],"total_pages":1,"total_results":1}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val repository = ListsRepositoryImpl(tmdbClient = TmdbClient(engine = mockEngine))

            val result = repository.getLists(ListsRepositoryTestConstant.ACCOUNT_ID, ListsRepositoryTestConstant.SESSION_ID, page = 1)

            assertEquals(1, result.list?.size)
            assertEquals("The Marvel Universe", result.list?.first()?.name)
        }

    @Test
    fun testGetListDetailsParsesItems() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/3/list/${ListsRepositoryTestConstant.LIST_ID}", request.url.encodedPath)
                    respond(
                        content =
                            """
                            {"name":"My List","description":"desc","items":[{"id":634649,"title":"Spider-Man: No Way Home","poster_path":"/p.jpg","release_date":"2021-12-15"}]}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val repository = ListsRepositoryImpl(tmdbClient = TmdbClient(engine = mockEngine))

            val result =
                repository.getListDetails(ListsRepositoryTestConstant.LIST_ID, ListsRepositoryTestConstant.SESSION_ID)

            assertEquals("My List", result.name)
            assertEquals(1, result.items.size)
            assertEquals("Spider-Man: No Way Home", result.items.first().title)
        }

    @Test
    fun testCreateListReturnsNewListId() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/3/list", request.url.encodedPath)
                    respond(
                        content = """{"success":true,"status_message":"Created.","list_id":5861}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val repository = ListsRepositoryImpl(tmdbClient = TmdbClient(engine = mockEngine))

            val listId = repository.createList(ListsRepositoryTestConstant.SESSION_ID, name = "My List", description = "desc")

            assertEquals(ListsRepositoryTestConstant.LIST_ID, listId)
        }

    @Test
    fun testAddMovieToListSucceeds() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "/3/list/${ListsRepositoryTestConstant.LIST_ID}/add_item",
                        request.url.encodedPath,
                    )
                    respond(
                        content = """{"status_code":12,"status_message":"Updated."}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val repository = ListsRepositoryImpl(tmdbClient = TmdbClient(engine = mockEngine))

            val result =
                repository.addMovieToList(
                    ListsRepositoryTestConstant.LIST_ID,
                    ListsRepositoryTestConstant.SESSION_ID,
                    ListsRepositoryTestConstant.MOVIE_ID,
                )

            assertEquals(12, result.statusCode)
        }

    @Test
    fun testDeleteListSucceeds() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/3/list/${ListsRepositoryTestConstant.LIST_ID}", request.url.encodedPath)
                    respond(
                        content = """{"status_code":12,"status_message":"Deleted."}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val repository = ListsRepositoryImpl(tmdbClient = TmdbClient(engine = mockEngine))

            val result = repository.deleteList(ListsRepositoryTestConstant.LIST_ID, ListsRepositoryTestConstant.SESSION_ID)

            assertEquals(12, result.statusCode)
        }
}
