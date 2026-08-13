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

private object TmdbClientConstant {
    const val TIME_OUT = 30000L

    // Deliberately much shorter than TIME_OUT. Connecting is the step that fails when a host is
    // unreachable (or DNS-hijacked to a blackhole address - see NetworkConstant.HOST), and it is
    // retried, so a 30s connect budget meant a dead host spun for ~3 minutes before the UI could
    // show its error card. Reading a slow-but-alive response still gets the full TIME_OUT.
    const val CONNECT_TIME_OUT = 10000L
    const val MAX_RETRIES = 3

    // The Logging plugin prints full request URLs, and TMDB auth travels as an `api_key` query
    // parameter, so an unscrubbed line would put the key in logcat/stdout on every request.
    val apiKeyRegex = Regex("(api_key=)[^&\\s]*")
    const val REDACTED_API_KEY = "$1<redacted>"
    const val LOG_TAG = "HTTP Client"
}

private val tmdbClientConfig: HttpClientConfig<*>.() -> Unit = {
    install(HttpRequestRetry) {
        maxRetries = TmdbClientConstant.MAX_RETRIES
        // Takes maxRetries, NOT a status code - passing HttpStatusCode.InternalServerError.value
        // here (as this used to) silently asked for 500 retries on every 5xx.
        retryOnServerErrors(maxRetries = TmdbClientConstant.MAX_RETRIES)
        retryOnException(maxRetries = TmdbClientConstant.MAX_RETRIES, retryOnTimeout = true)
    }
    // Left false deliberately: Ktor installs its own default response validation whenever
    // expectSuccess = true, and that default always runs before the custom HttpResponseValidator
    // below (HttpClient appends it after merging in this whole config block), so it would throw
    // its own ClientRequestException/ServerResponseException first and this client's HttpExceptions
    // would never actually be thrown. See TmdbClientTest.
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = TmdbClientConstant.CONNECT_TIME_OUT
        requestTimeoutMillis = TmdbClientConstant.TIME_OUT
        socketTimeoutMillis = TmdbClientConstant.TIME_OUT
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
                    val scrubbed = message.replace(TmdbClientConstant.apiKeyRegex, TmdbClientConstant.REDACTED_API_KEY)
                    Napier.d(tag = TmdbClientConstant.LOG_TAG) { scrubbed }
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
