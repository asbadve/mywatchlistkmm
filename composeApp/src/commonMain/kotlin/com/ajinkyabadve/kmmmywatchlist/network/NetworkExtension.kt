package com.ajinkyabadve.kmmmywatchlist.network

import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.http.HttpStatusCode

fun HttpExceptions.isServerError(): Boolean =
    when (this.response.status) {
        HttpStatusCode.Unauthorized -> false
        HttpStatusCode.Forbidden -> false
        HttpStatusCode.NotFound -> false
        HttpStatusCode.UpgradeRequired -> false
        HttpStatusCode.RequestTimeout -> false
        in HttpStatusCode.InternalServerError..HttpStatusCode.GatewayTimeout -> {
            false
        }
        else -> true
    }
