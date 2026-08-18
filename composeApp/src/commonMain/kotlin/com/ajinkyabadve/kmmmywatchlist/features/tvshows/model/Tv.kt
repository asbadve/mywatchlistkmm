package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvPageResult(
    @SerialName("page") var page: Int,
    @SerialName("results") var list: List<Tv>?,
    @SerialName("total_results") var totalResults: Int?,
    @SerialName("total_pages") var totalPages: Int?,
)

@Serializable
data class Tv(
    var id: Int = -1,
    var adult: Boolean = false,
    @SerialName("backdrop_path") var backdropPath: String? = "",
    @SerialName("original_language") var originalLanguage: String = "",
    @SerialName("original_title") var originalTitle: String = "",
    var overview: String = "",
    var popularity: Double = 0.0,
    @SerialName("poster_path") var posterPath: String? = "",
    @SerialName("first_air_date") var firstAirDate: String = "",
    @SerialName("name") var title: String = "",
    var video: Boolean = false,
    @SerialName("vote_average") var voteAverage: Double = 0.0,
    @SerialName("vote_count") var voteCount: Int = 0,
) {
    /**
     * True only for a real, parseable first-air date strictly after [today] - blank or unparsable
     * dates read as already released rather than incorrectly flagged upcoming. Mirrors
     * `SearchResultItem.isUpcoming`.
     */
    fun isUpcoming(today: LocalDate): Boolean {
        if (firstAirDate.isBlank()) return false
        return try {
            LocalDate.parse(firstAirDate) > today
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
