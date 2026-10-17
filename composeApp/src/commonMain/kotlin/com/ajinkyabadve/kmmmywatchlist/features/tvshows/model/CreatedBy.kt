package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatedBy(
    val id: Int = -1,
    val name: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
)
