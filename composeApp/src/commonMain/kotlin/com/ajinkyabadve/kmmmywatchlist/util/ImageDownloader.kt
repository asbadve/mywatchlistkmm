package com.ajinkyabadve.kmmmywatchlist.util

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

object ImageDownloader {
    private val client = HttpClient()

    suspend fun downloadAndSave(url: String): String? =
        try {
            Napier.d("Starting image download from: $url", tag = TAG)
            val response = client.get(url)
            val bytes = response.body<ByteArray>()
            val fileName = url.substringAfterLast("/").substringBefore("?")
            val finalName =
                if (fileName.isEmpty() || !fileName.contains(".")) {
                    "image_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}.jpg"
                } else {
                    fileName
                }
            Napier.d("Saving image bytes to $finalName", tag = TAG)
            ImageSaver().saveImage(bytes, finalName)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: io.ktor.client.plugins.ResponseException) {
            Napier.e("HTTP error downloading image", e, tag = TAG)
            null
        } catch (e: io.ktor.utils.io.errors.IOException) {
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
}
