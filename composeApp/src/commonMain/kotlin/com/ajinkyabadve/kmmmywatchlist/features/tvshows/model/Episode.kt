package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Episode(
    val id: Long = -1,
    val name: String = "",
    val overview: String = "",
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_number") val episodeNumber: Int = 0,
    @SerialName("season_number") val seasonNumber: Int = 0,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val runtime: Int? = null,
) {
    /**
     * True only for a real, parseable air date on or before [today] - a missing or unparsable date
     * reads as not-yet-released rather than incorrectly treated as already aired. Used to pick the
     * "current"/"latest" season and episode by verified air date instead of trusting TMDB's
     * `next_episode_to_air`/`last_episode_to_air` classification, which can lag once an episode's
     * date has actually passed.
     */
    fun isReleased(today: LocalDate): Boolean {
        val date = airDate
        if (date.isNullOrBlank()) return false
        return try {
            LocalDate.parse(date) <= today
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
