package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ReleaseDateItem
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import kotlinx.datetime.LocalDate

private object ReleaseDateResolutionConstant {
    /** TMDB's `release_dates[].release_date` is an ISO datetime (`2024-05-17T00:00:00.000Z`); the
     *  date part is the first 10 characters. */
    const val ISO_DATE_LENGTH = 10
}

/**
 * TMDB's movie release types (`release_dates.results[].release_dates[].type`), confirmed against
 * the live API and the Movie Release Dates reference page on 2026-10-03.
 */
enum class ReleaseType(
    val tmdbValue: Int,
) {
    PREMIERE(1),
    THEATRICAL_LIMITED(2),
    THEATRICAL(3),
    DIGITAL(4),
    PHYSICAL(5),
    TV(6),
    ;

    companion object {
        fun fromTmdbValue(value: Int): ReleaseType? = entries.firstOrNull { it.tmdbValue == value }
    }
}

/** The date a reminder should fire around, and which date it was. */
data class ResolvedRelease(
    val date: LocalDate,
    val source: ReleaseSource,
)

/** In priority order: what a "release" means to someone in a given region. Premiere (festival
 *  screenings) is deliberately absent - telling someone a film is out because it screened at a
 *  festival is worse than not telling them at all. */
private val RegionalReleasePriority =
    listOf(
        ReleaseType.THEATRICAL to ReleaseSourceKind.THEATRICAL,
        ReleaseType.THEATRICAL_LIMITED to ReleaseSourceKind.THEATRICAL,
        ReleaseType.DIGITAL to ReleaseSourceKind.DIGITAL,
    )

/**
 * Which date a movie's release reminder fires on. Looks in the viewer's selected region, then the
 * fallback region, for the earliest theatrical, then limited-theatrical, then digital date; falls
 * back to TMDB's primary `release_date` when neither region has one.
 *
 * "Earliest", not "first listed": a region's bucket also carries re-releases (seen live for movie
 * 550: a 2024 "25th Anniversary" theatrical and a 2026 "4K Remaster" alongside the 1999 original).
 * The primary date is preferred over an arbitrary third country's, which would be meaningless to
 * this viewer.
 */
fun MovieDetail.resolveReleaseDate(
    regionCode: String,
    fallbackRegionCode: String,
): ResolvedRelease? {
    val buckets = releaseDates?.results.orEmpty()
    listOf(regionCode, fallbackRegionCode).distinct().forEach { region ->
        val items = buckets.firstOrNull { it.iso3166 == region }?.releaseDates.orEmpty()
        RegionalReleasePriority.forEach { (type, kind) ->
            val earliest = items.filter { ReleaseType.fromTmdbValue(it.type) == type }.mapNotNull { it.parsedDate() }.minOrNull()
            if (earliest != null) return ResolvedRelease(earliest, ReleaseSource(kind, region))
        }
    }
    return parseDateOrNull(releaseDate)?.let { ResolvedRelease(it, ReleaseSource(ReleaseSourceKind.PRIMARY)) }
}

/** One dated release in a region, for the movie page's "Release dates" list. [note] is TMDB's
 *  free-text label (e.g. "4K Remaster", "Netflix"), blank when there is none. */
data class RegionalRelease(
    val type: ReleaseType,
    val date: LocalDate,
    val certification: String,
    val note: String,
)

/**
 * Every dated release TMDB lists for [regionCode], oldest first - what the movie page shows when
 * the release-date chip is tapped. Entries with an unknown type or an unparseable date are dropped.
 */
fun MovieDetail.regionalReleases(regionCode: String): List<RegionalRelease> =
    releaseDates
        ?.results
        .orEmpty()
        .firstOrNull { it.iso3166 == regionCode }
        ?.releaseDates
        .orEmpty()
        .mapNotNull { item ->
            val type = ReleaseType.fromTmdbValue(item.type) ?: return@mapNotNull null
            val date = item.parsedDate() ?: return@mapNotNull null
            RegionalRelease(type = type, date = date, certification = item.certification.trim(), note = item.note.trim())
        }.sortedBy { it.date }

/** A show's reminder fires on its first air date - TMDB has no per-region equivalent for TV. */
fun TvDetail.resolveReleaseDate(): ResolvedRelease? =
    parseDateOrNull(firstAirDate)?.let { ResolvedRelease(it, ReleaseSource(ReleaseSourceKind.FIRST_AIR)) }

/** Same rule as `Movie.isUpcoming`: a parseable primary release date after [today]. Blank or
 *  year-only dates read as not upcoming. */
fun MovieDetail.isUpcoming(today: LocalDate): Boolean = parseDateOrNull(releaseDate)?.let { it > today } ?: false

/** Same rule as `Tv.isUpcoming`, on the first air date. */
fun TvDetail.isUpcoming(today: LocalDate): Boolean = parseDateOrNull(firstAirDate)?.let { it > today } ?: false

private fun ReleaseDateItem.parsedDate(): LocalDate? = parseDateOrNull(releaseDate.take(ReleaseDateResolutionConstant.ISO_DATE_LENGTH))

internal fun parseDateOrNull(raw: String?): LocalDate? {
    if (raw.isNullOrBlank()) return null
    return try {
        LocalDate.parse(raw)
    } catch (e: IllegalArgumentException) {
        null
    }
}
