package com.ajinkyabadve.kmmmywatchlist.core.data

import com.ajinkyabadve.kmmmywatchlist.core.UiText

/**
 * Kotlin port of Google's `Resource<T>` (the "Guide to app architecture" sample / Android
 * Sunflower) - the type [NetworkBoundResource] emits. `Loading`/`Error` both optionally carry the
 * last-known [data] alongside (mirrors `Resource.loading(data)`/`Resource.error(msg, data)` in the
 * original), so a consumer can choose to keep showing cached content through a background refresh
 * or a refresh failure, without this type making that call itself.
 */
sealed interface Resource<out T> {
    data class Loading<T>(
        val data: T? = null,
    ) : Resource<T>

    data class Success<T>(
        val data: T,
    ) : Resource<T>

    data class Error<T>(
        val cause: Throwable,
        val message: UiText,
        val data: T? = null,
    ) : Resource<T>
}
