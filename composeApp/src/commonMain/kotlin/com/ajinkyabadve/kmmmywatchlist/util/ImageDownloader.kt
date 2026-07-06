package com.ajinkyabadve.kmmmywatchlist.util

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.github.aakira.napier.Napier

object ImageDownloader {
    private val client = HttpClient()

    suspend fun downloadAndSave(url: String): String? {
        return try {
            Napier.d("Starting image download from: $url", tag = "ImageDownloader")
            val response = client.get(url)
            val bytes = response.body<ByteArray>()
            val fileName = url.substringAfterLast("/").substringBefore("?")
            val finalName = if (fileName.isEmpty() || !fileName.contains(".")) {
                "image_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}.jpg"
            } else {
                fileName
            }
            Napier.d("Saving image bytes to $finalName", tag = "ImageDownloader")
            ImageSaver().saveImage(bytes, finalName)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: io.ktor.client.plugins.ResponseException) {
            Napier.e("HTTP error downloading image", e, tag = "ImageDownloader")
            null
        } catch (e: io.ktor.utils.io.errors.IOException) {
            Napier.e("Network/IO error downloading image", e, tag = "ImageDownloader")
            null
        } catch (e: Exception) {
            Napier.e("Unexpected error downloading image", e, tag = "ImageDownloader")
            null
        }
    }
}
