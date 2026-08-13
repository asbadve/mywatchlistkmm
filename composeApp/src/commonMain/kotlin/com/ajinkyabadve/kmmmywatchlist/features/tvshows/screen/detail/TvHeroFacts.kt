package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.constant.RegionConstant
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Episode
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail

private object TvStatusConstant {
    const val STATUS_RETURNING = "Returning Series"
    const val STATUS_IN_PRODUCTION = "In Production"
    const val STATUS_PLANNED = "Planned"
}

/**
 * Whether a series is still going, which is the one fact a film never needs and a viewer always
 * wants: it decides whether starting the show means signing up for an unfinished story.
 */
val TvDetail.isOngoing: Boolean
    get() =
        status == TvStatusConstant.STATUS_RETURNING ||
            status == TvStatusConstant.STATUS_IN_PRODUCTION ||
            status == TvStatusConstant.STATUS_PLANNED

/**
 * The episode the hero points at: the one airing next if there is one, otherwise the most recent.
 *
 * A returning series is best entered at what is coming; a finished one has no "next", so the last
 * episode is the honest thing to name.
 */
fun TvDetail.heroEpisode(): Episode? = nextEpisodeToAir ?: lastEpisodeToAir

/** The age rating TMDB publishes for the US, mirroring how the movie hero picks its certification. */
fun TvDetail.usContentRating(): String? =
    contentRatings
        ?.results
        ?.firstOrNull { it.iso3166 == RegionConstant.US }
        ?.rating
        ?.takeIf { it.isNotEmpty() }
