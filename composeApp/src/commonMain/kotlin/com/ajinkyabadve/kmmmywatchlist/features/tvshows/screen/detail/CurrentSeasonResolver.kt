package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import kotlinx.datetime.LocalDate

/**
 * The season/episode number of the latest already-aired episode across [seasonDetails], verified
 * against [today] rather than trusted from TMDB's `next_episode_to_air`/`last_episode_to_air`
 * classification - that classification can lag once an episode's date has actually passed, showing
 * an unreleased upcoming season as "current" instead of the season the viewer can actually watch.
 *
 * Falls back to the earliest season (no specific episode) when nothing has released yet, e.g. a
 * brand-new show whose only listed episodes are still in the future.
 */
internal fun resolveCurrentSeasonAndEpisode(
    seasonDetails: Map<Int, TvSeasonDetail>,
    today: LocalDate,
): Pair<Int?, Int?> {
    // Ties on air date (e.g. a whole season dropping on one day) break toward the higher
    // season/episode number, so among same-day episodes 1-4 this picks 4, not whichever happened
    // to sort first.
    val latestReleasedEpisode =
        seasonDetails.values
            .asSequence()
            .flatMap { it.episodes }
            .filter { it.isReleased(today) }
            .maxWithOrNull(compareBy({ it.airDate.orEmpty() }, { it.seasonNumber }, { it.episodeNumber }))

    if (latestReleasedEpisode != null) {
        return latestReleasedEpisode.seasonNumber to latestReleasedEpisode.episodeNumber
    }

    val earliestSeasonNumber =
        seasonDetails.keys.filter { it > 0 }.minOrNull()
            ?: seasonDetails.keys.minOrNull()
    return earliestSeasonNumber to null
}
