package com.ajinkyabadve.kmmmywatchlist.util

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock

object ImageDownloader {
    private val client = HttpClient()

    suspend fun downloadAndSave(url: String): String? =
        try {
            Napier.d("Starting image download from: $url", tag = TAG)
            val response = client.get(url)
            val bytes = response.body<ByteArray>()
            val fileName = url.substringAfterLast(PATH_SEPARATOR).substringBefore(QUERY_SEPARATOR)
            val finalName =
                if (fileName.isEmpty() || !fileName.contains(EXTENSION_SEPARATOR)) {
                    "$FALLBACK_NAME_PREFIX${Clock.System.now().toEpochMilliseconds()}$FALLBACK_EXTENSION"
                } else {
                    fileName
                }
            Napier.d("Saving image bytes to $finalName", tag = TAG)
            ImageSaver().saveImage(bytes, finalName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseException) {
            Napier.e("HTTP error downloading image", e, tag = TAG)
            null
        } catch (e: IOException) {
            Napier.e("Network/IO error downloading image", e, tag = TAG)
            null
        } catch (e: IllegalStateException) {
            Napier.e("Platform error saving image", e, tag = TAG)
            null
        } catch (e: IllegalArgumentException) {
            Napier.e("Platform error saving image", e, tag = TAG)
            null
        }

    private const val TAG = "ImageDownloader"
    private const val PATH_SEPARATOR = "/"
    private const val QUERY_SEPARATOR = "?"
    private const val EXTENSION_SEPARATOR = "."

    // Name for a URL that has no usable file name of its own.
    private const val FALLBACK_NAME_PREFIX = "image_"
    private const val FALLBACK_EXTENSION = ".jpg"
}
