package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

/**
 * The raw TMDB series statuses the TV detail tests drive, shared by the facts and hero tests.
 *
 * Deliberately a second declaration rather than an import of production's `TvStatusConstant`: these
 * strings are the contract under test - TMDB's exact wording, which the hero also renders verbatim -
 * and a test that imports the constant it is verifying would pass on any typo.
 */
object TvStatusTestConstant {
    const val RETURNING = "Returning Series"
    const val IN_PRODUCTION = "In Production"
    const val PLANNED = "Planned"
    const val ENDED = "Ended"
    const val CANCELED = "Canceled"
}
