package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.serialization.Serializable

@Serializable
data class TranslationsResponse(
    val translations: List<Translation> = emptyList(),
)
