package com.ajinkyabadve.kmmmywatchlist.network.builder

import MyWatchList.composeApp.BuildConfig
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant.API_KEY
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant.HOST
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant.PAGE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath

fun HttpRequestBuilder.mediaHttpBuilder(
    path: String,
    pageNumber: String,
    mediaType: String
) {
    url {
        protocol = URLProtocol.HTTPS
        host = HOST
        trailingQuery = true
        parameters.append(API_KEY, BuildConfig.TMDB_API_KEY)
        encodedPath = "$mediaType$path"
        parameters.append(PAGE, pageNumber)
    }
}
