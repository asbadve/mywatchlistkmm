package com.ajinkyabadve.kmmmywatchlist.network.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class TmdbClient {

    internal val client = HttpClient {
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
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }
    }

    object TmdbApiClient {
        val newInstance: TmdbClient = TmdbClient()
    }

    private companion object {
        const val TIME_OUT = 30000L

    }
}
