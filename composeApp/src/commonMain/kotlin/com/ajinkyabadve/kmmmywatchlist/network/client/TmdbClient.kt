package com.ajinkyabadve.kmmmywatchlist.network.client

import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.github.aakira.napier.Napier

class TmdbClient {
    internal val client =
        HttpClient {
            install(HttpRequestRetry) {
                maxRetries = 3
                retryOnServerErrors(HttpStatusCode.InternalServerError.value)
                retryOnException(maxRetries = 3, retryOnTimeout = true)
            }
            expectSuccess = true
            install(HttpTimeout) {
                val timeout = TIME_OUT
                connectTimeoutMillis = timeout
                requestTimeoutMillis = timeout
                socketTimeoutMillis = timeout
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        useAlternativeNames = true
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.d(tag = "HTTP Client") { message }
                    }
                }
                level = LogLevel.ALL
            }
            HttpResponseValidator {
                validateResponse {
                    if (!it.status.isSuccess()) {
                        val httpFailureReason =
                            when (it.status) {
                                HttpStatusCode.Unauthorized -> "Unauthorized request"
                                HttpStatusCode.Forbidden -> "${it.status.value} Missing API key"
                                HttpStatusCode.NotFound -> "Invalid Request"
                                HttpStatusCode.UpgradeRequired -> "Upgrade to VIP"
                                HttpStatusCode.RequestTimeout -> "Network Timeout"
                                HttpStatusCode.RequestTimeout -> "Network Timeout"
                                in HttpStatusCode.InternalServerError..HttpStatusCode.GatewayTimeout -> {
                                    "${it.status.value} Server Error"
                                }

                                else -> "Network error!"
                            }

                        throw HttpExceptions(
                            response = it,
                            cachedResponseText = it.bodyAsText(),
                            failureReason = httpFailureReason,
                        )
                    }
                }
            }
        }

    object TmdbApiClient {
        val newInstance: TmdbClient = TmdbClient()
    }

    private companion object {
        const val TIME_OUT = 30000L
    }
}
