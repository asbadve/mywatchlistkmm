package com.ajinkyabadve.kmmmywatchlist.core.usecase

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse

/**
 * Finds the watchable trailer URL in a title's videos, if there is one.
 *
 * Movies and TV shows share the [VideoResponse] model and had identical copies of this filter, so
 * it lives here once. Returns a full URL rather than a bare key: every caller wanted the URL, and
 * assembling it at each call site is how the two copies came to differ in the first place.
 */
class FindYoutubeTrailerUseCase {
    operator fun invoke(videos: VideoResponse?): String? {
        val key =
            videos
                ?.results
                ?.firstOrNull {
                    it.site.equals(YOUTUBE_SITE, ignoreCase = true) &&
                        it.type.equals(TRAILER_TYPE, ignoreCase = true)
                }?.key
                ?.takeIf { it.isNotEmpty() }
        return key?.let { YOUTUBE_WATCH_URL + it }
    }

    private companion object {
        const val YOUTUBE_SITE = "YouTube"
        const val TRAILER_TYPE = "Trailer"
        const val YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v="
    }
}
