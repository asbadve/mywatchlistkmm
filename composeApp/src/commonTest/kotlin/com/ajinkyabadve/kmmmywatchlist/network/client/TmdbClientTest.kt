package com.ajinkyabadve.kmmmywatchlist.network.client

import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TmdbClientTest {
    /**
     * Regression test: TmdbClient used to set `expectSuccess = true`, which made Ktor install its
     * own default response validation on top of this client's custom one. That default validation
     * always wins the race and throws its own ClientRequestException/ServerResponseException, so
     * the client's own HttpExceptions (with the friendly per-status failure message) was never
     * actually thrown by a real request. See the `expectSuccess = false` comment in TmdbClient.
     */
    @Test
    fun testNonSuccessResponseThrowsHttpExceptionsWithFriendlyMessage() =
        runTest {
            val mockEngine = MockEngine { respond(content = "", status = HttpStatusCode.NotFound, headers = headersOf()) }
            val tmdbClient = TmdbClient(mockEngine)

            val exception =
                assertFailsWith<HttpExceptions> {
                    tmdbClient.client.get("https://mock.test/x")
                }

            assertEquals("Status: 404 Not Found. Failure: Invalid Request", exception.message)
        }

    /**
     * Regression test: the retry plugin used to be configured with
     * `retryOnServerErrors(HttpStatusCode.InternalServerError.value)`. That parameter is
     * `maxRetries`, not a status code, so passing 500 asked for *500 retries* on every 5xx and
     * silently overrode the `maxRetries = 3` set on the line above it.
     */
    @Test
    fun testServerErrorsAreRetriedThreeTimesNotFiveHundred() =
        runTest {
            var attempts = 0
            val mockEngine =
                MockEngine {
                    attempts++
                    respond(content = "", status = HttpStatusCode.InternalServerError, headers = headersOf())
                }
            val tmdbClient = TmdbClient(mockEngine)

            assertFailsWith<HttpExceptions> {
                tmdbClient.client.get("https://mock.test/x")
            }

            // 1 initial attempt + 3 retries.
            assertEquals(4, attempts)
        }
}
