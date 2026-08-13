package com.ajinkyabadve.kmmmywatchlist.features.person.model

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.BackdropImage
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.ExternalIds
import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearsUntil
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// One entry of /3/person/{id}?append_to_response=combined_credits - a movie or TV credit merged
// into a single shape. Movies carry title/release_date, TV carries name/first_air_date; cast
// entries carry character, crew entries carry job/department. media_type discriminates.
@Serializable
data class PersonCredit(
    val id: Long = -1,
    @SerialName("media_type") val mediaType: String = "",
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val popularity: Double = 0.0,
    val character: String? = null,
    val job: String? = null,
    val department: String? = null,
    @SerialName("credit_id") val creditId: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
) {
    val displayTitle: String get() = title ?: name ?: originalTitle ?: originalName ?: ""
    val displayDate: String? get() = releaseDate ?: firstAirDate
    val isMovie: Boolean get() = mediaType == MediaTypeConstant.MOVIE

    // Short user-facing hint for whether a credit is a film or a series.
    val mediaTypeLabel: String?
        get() =
            when (mediaType) {
                MediaTypeConstant.MOVIE -> "Movie"
                MediaTypeConstant.TV -> "TV"
                else -> null
            }
}

@Serializable
data class PersonCombinedCredits(
    val cast: List<PersonCredit> = emptyList(),
    val crew: List<PersonCredit> = emptyList(),
)

/**
 * TMDB-style filmography: "Acting" first (cast credits), then one section per crew department,
 * each sorted newest-first with undated (upcoming/unknown) entries on top - mirroring how
 * themoviedb.org person pages present credits. Pass [mediaType] ([MediaTypeConstant.MOVIE] or
 * [MediaTypeConstant.TV]) to narrow the filmography to one medium, like the media filter on
 * themoviedb.org.
 */
fun PersonCombinedCredits.filmographySections(mediaType: String? = null): List<Pair<String, List<PersonCredit>>> {
    fun List<PersonCredit>.byMediaType() = if (mediaType == null) this else filter { it.mediaType == mediaType }

    val sections = mutableListOf<Pair<String, List<PersonCredit>>>()
    val filteredCast = cast.byMediaType()
    if (filteredCast.isNotEmpty()) sections += "Acting" to filteredCast.sortedForFilmography()
    crew
        .byMediaType()
        .groupBy { credit -> credit.department?.takeIf { it.isNotEmpty() } ?: "Other" }
        .toList()
        .sortedBy { (department, _) -> department }
        .forEach { (department, credits) -> sections += department to credits.sortedForFilmography() }
    return sections
}

/**
 * The credits a person is best known for, mirroring themoviedb.org: someone known for a crew
 * department (e.g. Christopher Nolan - Directing) gets that department's credits, actors get cast
 * credits; ranked by vote count so all-time hits outrank talk-show appearances and cameos.
 */
fun PersonDetail.knownForCredits(max: Int = 20): List<PersonCredit> {
    val credits = combinedCredits ?: return emptyList()
    val department = knownForDepartment
    val pool =
        if (department.isNullOrEmpty() || department == "Acting") {
            credits.cast
        } else {
            credits.crew.filter { it.department == department }
        }
    return pool
        .ifEmpty { credits.cast + credits.crew }
        .distinctBy { it.id }
        .sortedByDescending { it.voteCount }
        .take(max)
}

/**
 * The credit whose backdrop stands in as this person's hero image.
 *
 * TMDB people have no backdrop of their own - only portrait profiles - so the banner borrows one
 * from the work they are best known for: Forrest Gump for Tom Hanks, Interstellar for Christopher
 * Nolan. [knownForCredits] already ranks by vote count precisely so all-time hits outrank talk-show
 * appearances, which is exactly the ordering wanted here; ranking by `popularity` instead surfaces
 * whichever chat show they appeared on last week.
 *
 * Free of extra network calls - `combined_credits` is already appended to the person request.
 * Returns null for people with no credits carrying a backdrop, so callers must have a plain layout
 * to fall back to.
 */
fun PersonDetail.heroBackdropCredit(): PersonCredit? = knownForCredits().firstOrNull { !it.backdropPath.isNullOrEmpty() }

/**
 * Year of this person's earliest film credit - how long they have been working, as one number.
 *
 * Films only, and that restriction is the whole point. A TV credit carries the *series*
 * `first_air_date`, not the date of the episode this person was in, so a single guest slot on a
 * long-running show back-dates them by decades: Tom Holland came out as first working in 1988,
 * eight years before he was born. A film's `release_date` is the credit's own date, so it can be
 * trusted.
 *
 * Deliberately not a "breakout year" either: TMDB has no such field, and deriving one would mean
 * guessing which credit counted as the break.
 */
fun PersonDetail.firstFilmYear(): Int? {
    val credits = combinedCredits ?: return null
    return (credits.cast + credits.crew)
        .filter { it.isMovie }
        .mapNotNull { it.releaseDate?.take(4)?.toIntOrNull() }
        .filter { it > 0 }
        .minOrNull()
}

private fun List<PersonCredit>.sortedForFilmography(): List<PersonCredit> =
    sortedWith(compareByDescending { it.displayDate?.takeIf { date -> date.isNotEmpty() } ?: "9999" })

/** Whole years between two ISO yyyy-mm-dd dates, or null when either doesn't parse. */
fun yearsBetween(
    fromDate: String?,
    toDate: String?,
): Int? {
    if (fromDate.isNullOrEmpty() || toDate.isNullOrEmpty()) return null
    return try {
        LocalDate.parse(fromDate).yearsUntil(LocalDate.parse(toDate))
    } catch (_: IllegalArgumentException) {
        null
    }
}

// The person images sub-resource returns a single "profiles" bucket.
@Serializable
data class PersonImagesResponse(
    val profiles: List<BackdropImage> = emptyList(),
)

// Person translations localize name/biography, unlike the media-level ones (name/overview).
@Serializable
data class PersonTranslationData(
    val name: String = "",
    val biography: String = "",
)

@Serializable
data class PersonTranslation(
    @SerialName("iso_3166_1") val iso3166: String = "",
    @SerialName("iso_639_1") val iso639: String = "",
    val name: String = "",
    @SerialName("english_name") val englishName: String = "",
    val data: PersonTranslationData = PersonTranslationData(),
)

@Serializable
data class PersonTranslationsResponse(
    val translations: List<PersonTranslation> = emptyList(),
)

// Note: TMDB's /3/person/{id} supports combined_credits, movie_credits, tv_credits, external_ids,
// images, translations, changes and latest as append_to_response values (per the person-details
// OpenAPI definition). combined_credits supersedes the separate movie/tv credit lists here.
@Serializable
data class PersonDetail(
    val id: Long = -1,
    val name: String = "",
    val adult: Boolean = false,
    @SerialName("also_known_as") val alsoKnownAs: List<String> = emptyList(),
    val biography: String = "",
    val birthday: String? = null,
    val deathday: String? = null,
    val gender: Int = 0,
    val homepage: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    val popularity: Double = 0.0,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("combined_credits") val combinedCredits: PersonCombinedCredits? = null,
    @SerialName("external_ids") val externalIds: ExternalIds? = null,
    val images: PersonImagesResponse? = null,
    val translations: PersonTranslationsResponse? = null,
) {
    // TMDB gender codes: 0 = not specified, 1 = female, 2 = male, 3 = non-binary.
    val genderLabel: String?
        get() =
            when (gender) {
                1 -> "Female"
                2 -> "Male"
                3 -> "Non-binary"
                else -> null
            }

    // "Known Credits" on themoviedb.org person pages: every cast and crew credit combined.
    val knownCreditsCount: Int
        get() = (combinedCredits?.cast?.size ?: 0) + (combinedCredits?.crew?.size ?: 0)
}
