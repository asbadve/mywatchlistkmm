package com.ajinkyabadve.kmmmywatchlist.features.search.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The three `media_type` values `/3/search/multi` actually returns (verified against the live API
 * on 2026-08-04). Anything else is a type this app has no detail screen for, so results carrying
 * an unrecognized value are dropped rather than rendered as an untappable card.
 */
enum class SearchMediaType(
    val apiValue: String,
) {
    MOVIE(API_VALUE_MOVIE),
    TV(API_VALUE_TV),
    PERSON(API_VALUE_PERSON),
    ;

    companion object {
        fun fromApiValue(value: String?): SearchMediaType? = entries.firstOrNull { it.apiValue == value }
    }
}

@Serializable
data class SearchPageResult(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val list: List<SearchResultItem>? = null,
    @SerialName("total_results") val totalResults: Int? = null,
    @SerialName("total_pages") val totalPages: Int? = null,
)

/**
 * One `/3/search/multi` result. The endpoint returns a heterogeneous array - movies carry
 * `title`/`release_date`/`poster_path`, TV carries `name`/`first_air_date`/`poster_path`, and
 * people carry `name`/`profile_path` and no date at all - so every type-specific field is
 * nullable here and the shared shape is exposed through [displayTitle] / [imagePath] / [mediaType].
 */
@Serializable
data class SearchResultItem(
    @SerialName("id") val id: Int = -1,
    @SerialName("media_type") val mediaTypeRaw: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("popularity") val popularity: Double = 0.0,
) {
    val mediaType: SearchMediaType?
        get() = SearchMediaType.fromApiValue(mediaTypeRaw)

    /** Movies title themselves with `title`; TV shows and people both use `name`. */
    val displayTitle: String
        get() = title ?: name.orEmpty()

    /** People have a `profile_path` where movies and TV shows have a `poster_path`. */
    val imagePath: String?
        get() = posterPath ?: profilePath

    /** `YYYY-MM-DD` from whichever date field this media type uses; people have neither. */
    val releaseYear: String?
        get() = (releaseDate ?: firstAirDate)?.take(YEAR_LENGTH)?.takeIf { it.isNotBlank() }

    /**
     * Identity of a result across pages. `id` alone isn't unique - TMDB numbers movies, TV shows
     * and people in separate spaces, so a movie and a person can share an id.
     */
    val uniqueKey: String
        get() = "$mediaTypeRaw-$id"
}

private const val API_VALUE_MOVIE = "movie"
private const val API_VALUE_TV = "tv"
private const val API_VALUE_PERSON = "person"
private const val YEAR_LENGTH = 4
