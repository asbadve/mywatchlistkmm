package com.ajinkyabadve.kmmmywatchlist.features.settings.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One entry from `/3/watch/providers/regions` - regions TMDB actually has watch-provider data for. */
@Serializable
data class WatchProviderRegion(
    @SerialName("iso_3166_1") val iso3166: String = "",
    @SerialName("english_name") val englishName: String = "",
    @SerialName("native_name") val nativeName: String = "",
)

@Serializable
data class WatchProviderRegionsResponse(
    val results: List<WatchProviderRegion> = emptyList(),
)
