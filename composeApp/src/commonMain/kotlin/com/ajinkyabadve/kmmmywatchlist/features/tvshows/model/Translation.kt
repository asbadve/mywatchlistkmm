package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Translation(
    @SerialName("iso_3166_1") val iso3166: String = "",
    @SerialName("iso_639_1") val iso639: String = "",
    val name: String = "",
    @SerialName("english_name") val englishName: String = "",
    val data: TranslationData = TranslationData(),
)
