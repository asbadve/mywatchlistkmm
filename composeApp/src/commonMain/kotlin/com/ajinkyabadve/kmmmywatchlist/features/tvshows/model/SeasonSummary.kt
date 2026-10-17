package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeasonSummary(
    val id: Long = -1,
    val name: String = "",
    @SerialName("season_number") val seasonNumber: Int = 0,
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    val overview: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0,
)
