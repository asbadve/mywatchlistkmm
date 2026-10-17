package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContentRating(
    @SerialName("iso_3166_1") val iso3166: String = "",
    val rating: String = "",
)
