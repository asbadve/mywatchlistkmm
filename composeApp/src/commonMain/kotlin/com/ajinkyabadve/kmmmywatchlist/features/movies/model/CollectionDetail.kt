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
        get() =
            parts
                .filter { it.voteAverage > 0 }
                .takeIf { it.isNotEmpty() }
                ?.let { rated -> rated.sumOf { it.voteAverage } / rated.size }

    // Parts sorted oldest-first (release order), undated entries (unreleased) last.
    val partsInReleaseOrder: List<Movie>
        get() = parts.sortedWith(compareBy { it.releaseDate.takeIf { date -> date.isNotEmpty() } ?: "9999" })
}

/**
 * Featured cast across a collection's movies, like themoviedb.org collection pages: actors ranked
 * by how many of the films they appear in, then by their best billing position. Characters from
 * all appearances are merged into one label.
 */
fun aggregateFeaturedCast(
    creditsPerMovie: List<Credits>,
    max: Int = 15,
): List<CastMember> =
    creditsPerMovie
        .flatMap { it.cast }
        .groupBy { it.id }
        .values
        .sortedWith(
            compareByDescending<List<CastMember>> { appearances -> appearances.size }
                .thenBy { appearances -> appearances.minOf { it.order } },
        ).take(max)
        .map { appearances ->
            val first = appearances.first()
            first.copy(
                character =
                    appearances
                        .map { it.character }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .joinToString(" / "),
            )
        }

/**
 * Featured crew across a collection's movies: directors first, then writers, then everyone else,
 * each group ranked by appearance count. All jobs a person held are merged into one label, and the
 * result is mapped onto [CastMember] so the shared cast row can render it (character = jobs).
 */
fun aggregateFeaturedCrew(
    creditsPerMovie: List<Credits>,
    max: Int = 15,
): List<CastMember> =
    creditsPerMovie
        .flatMap { it.crew }
        .groupBy { it.id }
        .values
        .sortedWith(
            compareBy<List<com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.CrewMember>> { jobs ->
                when {
                    jobs.any { it.job == JOB_DIRECTOR } -> 0
                    jobs.any { it.department == DEPARTMENT_WRITING } -> 1
                    else -> 2
                }
            }.thenByDescending { jobs -> jobs.size },
        ).take(max)
        .map { jobs ->
            val first = jobs.first()
            CastMember(
                id = first.id.toInt(),
                name = first.name,
                profilePath = first.profilePath,
                character =
                    jobs
                        .map { it.job }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .joinToString(", "),
            )
        }

// TMDB job/department identifiers (API values, not user-facing).
private const val JOB_DIRECTOR = "Director"
private const val DEPARTMENT_WRITING = "Writing"
