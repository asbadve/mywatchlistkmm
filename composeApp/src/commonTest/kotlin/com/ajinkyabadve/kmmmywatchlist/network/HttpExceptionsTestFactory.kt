package com.ajinkyabadve.kmmmywatchlist.network

import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.isSuccess

/**
 * Builds a real [HttpExceptions] for the given [statusCode] by driving it through an actual
 * Ktor request/response cycle (via [MockEngine]) instead of hand-constructing one, since
 * [HttpExceptions] requires a genuine [io.ktor.client.statement.HttpResponse]. Must be called
 * from a coroutine (e.g. inside `runTest { }`) since `runBlocking` isn't available on all
 * targets this module compiles commonTest for (notably JS).
 */
object HttpExceptionsTestFactory {
    suspend fun create(statusCode: HttpStatusCode): HttpExceptions {
        val mockEngine = MockEngine { respond(content = "", status = statusCode, headers = headersOf()) }
        // expectSuccess is deliberately left false: Ktor's own default response validation (triggered by
        // expectSuccess = true) would otherwise win over this custom validator and throw its own
        // ClientRequestException/ServerResponseException instead of HttpExceptions - see TmdbClient.
        val client =
            HttpClient(mockEngine) {
                HttpResponseValidator {
                    validateResponse {
                        if (!it.status.isSuccess()) {
                            throw HttpExceptions(
                                response = it,
                                cachedResponseText = it.bodyAsText(),
                                failureReason = "Mock failure",
                            )
                        }
                    }
                }
            }
        return try {
            client.get("https://mock.test")
            error("Expected HttpExceptions to be thrown for status $statusCode")
        } catch (e: HttpExceptions) {
            e
        }
    }
}
