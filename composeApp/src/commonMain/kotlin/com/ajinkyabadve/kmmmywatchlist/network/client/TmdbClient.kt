package com.ajinkyabadve.kmmmywatchlist.network.client

import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val TIME_OUT = 30000L

private val tmdbClientConfig: HttpClientConfig<*>.() -> Unit = {
    install(HttpRequestRetry) {
        maxRetries = 3
        retryOnServerErrors(HttpStatusCode.InternalServerError.value)
        retryOnException(maxRetries = 3, retryOnTimeout = true)
    }
    // Left false deliberately: Ktor installs its own default response validation whenever
    // expectSuccess = true, and that default always runs before the custom HttpResponseValidator
    // below (HttpClient appends it after merging in this whole config block), so it would throw
    // its own ClientRequestException/ServerResponseException first and this client's HttpExceptions
    // would never actually be thrown. See TmdbClientTest.
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = TIME_OUT
        requestTimeoutMillis = TIME_OUT
        socketTimeoutMillis = TIME_OUT
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
        logger =
            object : Logger {
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

class TmdbClient(
    engine: HttpClientEngine? = null,
) {
    internal val client: HttpClient =
        if (engine != null) HttpClient(engine, tmdbClientConfig) else HttpClient(tmdbClientConfig)

    object TmdbApiClient {
        val newInstance: TmdbClient = TmdbClient()
    }
}
