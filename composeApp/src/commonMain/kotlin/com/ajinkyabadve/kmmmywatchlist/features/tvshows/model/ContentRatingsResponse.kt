package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.serialization.Serializable

@Serializable
data class ContentRatingsResponse(
    val results: List<ContentRating> = emptyList(),
)
