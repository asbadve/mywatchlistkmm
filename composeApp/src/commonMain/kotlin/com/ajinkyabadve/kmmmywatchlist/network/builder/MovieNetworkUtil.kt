package com.ajinkyabadve.kmmmywatchlist.network.builder

import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant.API_KEY
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant.HOST
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant.PAGE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import kotlinproject.composeapp.BuildConfig

fun HttpRequestBuilder.mediaHttpBuilder(
    path: String,
    pageNumber: String,
    mediaType: String,
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

fun HttpRequestBuilder.trendingMediaHttpBuilder(
    timeWindow: String,
    mediaType: String, // movie/tv/people/all
    path: String, // trending
) {
    url {
        protocol = URLProtocol.HTTPS
        host = HOST
        trailingQuery = true
        encodedPath = "$path/$mediaType/$timeWindow"
        parameters.append(API_KEY, BuildConfig.TMDB_API_KEY)
    }
}
