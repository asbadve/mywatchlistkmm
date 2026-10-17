package com.ajinkyabadve.kmmmywatchlist.features.account.model

/** Which date [UpcomingMediaItem.date] represents - a closed set, so an enum rather than a
 *  boolean/string, per code-conventions §9. */
enum class UpcomingDateKind {
    MOVIE_RELEASE,
    NEXT_EPISODE,
}
