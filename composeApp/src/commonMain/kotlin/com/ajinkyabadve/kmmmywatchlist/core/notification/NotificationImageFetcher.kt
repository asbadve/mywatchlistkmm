package com.ajinkyabadve.kmmmywatchlist.core.notification

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException

/**
 * Downloads a notification's poster/still image as raw bytes - shared by the platform `actual`s
 * that can render an image inline (Android's `BigPictureStyle`, iOS's `UNNotificationAttachment`).
 * Same exception shape as `util/ImageDownloader.kt` (a plain image GET, not a TMDB API call, so
 * the app's usual `HttpExceptions`/`ContentConvertException` wrapping doesn't apply here).
 * Best-effort: any failure returns null so the caller falls back to a text-only notification
 * instead of losing the notification entirely.
 */
internal object NotificationImageFetcher {
    private const val TAG = "NotificationImageFetcher"
    private val client = HttpClient()

    suspend fun fetchBytes(url: String): ByteArray? =
        try {
            client.get(url).body<ByteArray>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseException) {
            Napier.e("HTTP error downloading notification image", e, tag = TAG)
            null
        } catch (e: IOException) {
            Napier.e("Network error downloading notification image", e, tag = TAG)
            null
        } catch (e: IllegalArgumentException) {
            Napier.e("Invalid notification image URL", e, tag = TAG)
            null
        }
}
