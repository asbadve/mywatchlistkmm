package com.ajinkyabadve.kmmmywatchlist.features.account.model

/** One currently tracked TV show, as read by [TrackedMediaRepository.observeTrackedTvShows]. */
data class TrackedTvShowSummary(
    val id: Int,
    val title: String,
    val posterPath: String?,
)
