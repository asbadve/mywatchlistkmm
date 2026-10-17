package com.ajinkyabadve.kmmmywatchlist.features.account.repository

/**
 * What a `trackedMedia` row needs about a title, taken from whichever screen the user favorited or
 * watchlisted it on - see [TrackedMediaRepository.trackLocally].
 *
 * @param releaseDate a movie's release date or a show's first-air date, `yyyy-MM-dd`; null when
 *   TMDB has none yet. The same unification `refreshAll` writes (`releaseDate ?: firstAirDate`).
 */
data class TrackedMediaSnapshot(
    val id: Int,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val voteAverage: Double,
)
