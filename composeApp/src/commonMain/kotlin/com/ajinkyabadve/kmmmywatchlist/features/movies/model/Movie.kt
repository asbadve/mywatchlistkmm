package com.ajinkyabadve.kmmmywatchlist.features.movies.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class MoviePageResult(
    @SerialName("page") var page: Int,
    @SerialName("results") var list: List<Movie>?,
    @SerialName("total_results") var totalResults: Int?,
    @SerialName("total_pages") var totalPages: Int,
)

@ExperimentalSerializationApi
@Serializable
data class Movie(
    var id: Int = -1,
    var adult: Boolean = false,
    @SerialName("backdrop_path") var backdropPath: String? = "",
    @SerialName("original_language") var originalLanguage: String = "",
    @SerialName("original_title") var originalTitle: String = "",
    var overview: String = "",
    var popularity: Double = 0.0,
    @JsonNames("poster_path", "profile_path")
    var posterPath: String? = "",
    @SerialName("release_date") var releaseDate: String = "",
    @JsonNames("title", "name")
    var title: String = "",
    var video: Boolean = false,
    @SerialName("vote_average") var voteAverage: Double = 0.0,
    @SerialName("vote_count") var voteCount: Int = 0,
    @SerialName("media_type") var media: String? = null,
) {
    /**
     * True only for a real, parseable release date strictly after [today] - blank or unparsable
     * dates read as already released rather than incorrectly flagged upcoming. Mirrors
     * `SearchResultItem.isUpcoming`.
     */
    fun isUpcoming(today: LocalDate): Boolean {
        if (releaseDate.isBlank()) return false
        return try {
            LocalDate.parse(releaseDate) > today
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
