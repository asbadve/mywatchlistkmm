package com.ajinkyabadve.kmmmywatchlist.features.trending.model

/** A title whose videos are fetched to find its newest trailer for the trailer rail. */
internal data class TrailerCandidate(
    val mediaId: Long,
    val isMovie: Boolean,
    val title: String,
    val backdropPath: String?,
)
