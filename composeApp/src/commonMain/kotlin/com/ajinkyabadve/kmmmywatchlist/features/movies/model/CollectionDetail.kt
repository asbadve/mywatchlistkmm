package com.ajinkyabadve.kmmmywatchlist.features.movies.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Note: TMDB's /3/collection/{id} supports images and translations as append_to_response values
// (per the collection-details OpenAPI definition). The movies belonging to the collection arrive
// in "parts", using the same shape as every other movie list.
@Serializable
data class CollectionDetail(
    val id: Long = -1,
    val name: String = "",
    val overview: String = "",
    @SerialName("original_language") val originalLanguage: String = "",
    @SerialName("original_name") val originalName: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val parts: List<Movie> = emptyList(),
    val images: ImagesResponse? = null,
    val translations: TranslationsResponse? = null,
) {
    // Average of the rated parts, matching the collection score themoviedb.org displays.
    val averageVote: Double?
        get() = parts
            .filter { it.voteAverage > 0 }
            .takeIf { it.isNotEmpty() }
            ?.let { rated -> rated.sumOf { it.voteAverage } / rated.size }

    // Parts sorted oldest-first (release order), undated entries (unreleased) last.
    val partsInReleaseOrder: List<Movie>
        get() = parts.sortedWith(compareBy { it.releaseDate.takeIf { date -> date.isNotEmpty() } ?: "9999" })
}
