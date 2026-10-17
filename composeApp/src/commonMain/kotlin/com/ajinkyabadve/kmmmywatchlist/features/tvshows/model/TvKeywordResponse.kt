package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Keyword
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvKeywordResponse(
    @SerialName("results") val keywords: List<Keyword> = emptyList(),
)
