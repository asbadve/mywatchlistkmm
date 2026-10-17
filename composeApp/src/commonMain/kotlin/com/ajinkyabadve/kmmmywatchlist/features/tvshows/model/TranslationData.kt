package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.serialization.Serializable

@Serializable
data class TranslationData(
    val name: String = "",
    val overview: String = "",
)
