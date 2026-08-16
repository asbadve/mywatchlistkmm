package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private object AccountMediaRepositoryTestConstant {
    const val ACCOUNT_ID = 100L
    const val SESSION_ID = "session_abc"
    const val MOVIE_ID = 11L
    const val TV_ID = 1399L
}

class AccountMediaRepositoryTest {
    @Test
    fun testGetFavoriteMoviesParsesPageAndResults() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "/3/account/${AccountMediaRepositoryTestConstant.ACCOUNT_ID}/favorite/movies",
                        request.url.encodedPath,
                    )
                    assertEquals(AccountMediaRepositoryTestConstant.SESSION_ID, request.url.parameters["session_id"])
                    respond(
                        content =
                            """
                            {"page":1,"results":[{"id":11,"title":"Star Wars","poster_path":"/p.jpg","release_date":"1977-05-25"}],"total_pages":1,"total_results":1}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val repository = AccountMediaRepositoryImpl(tmdbClient = TmdbClient(engine = mockEngine))

            val result =
                repository.getFavoriteMovies(
                    AccountMediaRepositoryTestConstant.ACCOUNT_ID,
                    AccountMediaRepositoryTestConstant.SESSION_ID,
                    page = 1,
                )

            assertEquals(1, result.list?.size)
            assertEquals("Star Wars", result.list?.first()?.displayTitle)
        }

    @Test
    fun testGetWatchlistTvParsesTvShapedFields() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "/3/account/${AccountMediaRepositoryTestConstant.ACCOUNT_ID}/watchlist/tv",
                        request.url.encodedPath,
                    )
                    respond(
                        content =
                            """
                            {"page":1,"results":[{"id":1399,"name":"Game of Thrones","poster_path":"/g.jpg","first_air_date":"2011-04-17"}],"total_pages":1,"total_results":1}
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val repository = AccountMediaRepositoryImpl(tmdbClient = TmdbClient(engine = mockEngine))

            val result =
                repository.getWatchlistTv(
                    AccountMediaRepositoryTestConstant.ACCOUNT_ID,
                    AccountMediaRepositoryTestConstant.SESSION_ID,
                    page = 1,
                )

            assertEquals("Game of Thrones", result.list?.first()?.displayTitle)
            assertEquals("2011", result.list?.first()?.releaseYear)
        }

    @Test
    fun testSetFavoriteSendsExpectedBody() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "/3/account/${AccountMediaRepositoryTestConstant.ACCOUNT_ID}/favorite",
                        request.url.encodedPath,
                    )
                    respond(
                        content = """{"status_code":1,"status_message":"Success."}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val repository = AccountMediaRepositoryImpl(tmdbClient = TmdbClient(engine = mockEngine))

            val result =
                repository.setFavorite(
                    AccountMediaRepositoryTestConstant.ACCOUNT_ID,
                    AccountMediaRepositoryTestConstant.SESSION_ID,
                    mediaType = "movie",
                    mediaId = 11L,
                    favorite = true,
                )

            assertEquals(1, result.statusCode)
        }

    @Test
    fun testGetAccountStatesHitsMoviePathAndParsesFlags() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "/3/movie/${AccountMediaRepositoryTestConstant.MOVIE_ID}/account_states",
                        request.url.encodedPath,
                    )
                    assertEquals(AccountMediaRepositoryTestConstant.SESSION_ID, request.url.parameters["session_id"])
                    respond(
                        content = """{"favorite":true,"watchlist":false}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val repository = AccountMediaRepositoryImpl(tmdbClient = TmdbClient(engine = mockEngine))

            val result =
                repository.getAccountStates(
                    AccountMediaRepositoryTestConstant.SESSION_ID,
                    MediaTypeConstant.MOVIE,
                    AccountMediaRepositoryTestConstant.MOVIE_ID,
                )

            assertTrue(result.favorite)
            assertFalse(result.watchlist)
        }

    @Test
    fun testGetAccountStatesHitsTvPath() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals(
                        "/3/tv/${AccountMediaRepositoryTestConstant.TV_ID}/account_states",
                        request.url.encodedPath,
                    )
                    respond(
                        content = """{"favorite":false,"watchlist":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val repository = AccountMediaRepositoryImpl(tmdbClient = TmdbClient(engine = mockEngine))

            val result =
                repository.getAccountStates(
                    AccountMediaRepositoryTestConstant.SESSION_ID,
                    MediaTypeConstant.TV,
                    AccountMediaRepositoryTestConstant.TV_ID,
                )

            assertFalse(result.favorite)
            assertTrue(result.watchlist)
        }
}
