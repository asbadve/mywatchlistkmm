package com.ajinkyabadve.kmmmywatchlist.features.trending.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult

/** The source filters the TMDB homepage offers for its Latest Trailers rail. */
enum class TrailerSource {
    IN_THEATERS,
    UPCOMING,
    POPULAR,
    ON_TV,
}

// One card in the "Latest Trailers" rail: the newest trailer of one movie or TV show, paired with
// the media it belongs to so the card can show title/backdrop and link into the app.
data class Trailer(
    val mediaId: Long,
    val isMovie: Boolean,
    val mediaTitle: String,
    val backdropPath: String?,
    val video: VideoResult,
)

private const val SITE_YOUTUBE = "YouTube"
private const val TYPE_TRAILER = "Trailer"
private const val TYPE_TEASER = "Teaser"

/**
 * Picks the video the TMDB homepage would feature for a title: YouTube only, trailers before
 * teasers, official before unofficial, newest first. Null when a title has nothing playable.
 */
fun latestTrailerVideo(videos: List<VideoResult>): VideoResult? =
    videos
        .filter { it.site == SITE_YOUTUBE && it.key.isNotEmpty() && (it.type == TYPE_TRAILER || it.type == TYPE_TEASER) }
        .minWithOrNull(
            compareBy<VideoResult> { if (it.type == TYPE_TRAILER) 0 else 1 }
                .thenBy { if (it.official) 0 else 1 }
                .thenByDescending { it.publishedAt },
        )

/** Watch URL for a picked video. */
fun VideoResult.youtubeUrl(): String = "https://www.youtube.com/watch?v=$key"
