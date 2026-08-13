package com.ajinkyabadve.kmmmywatchlist.core.constant

/**
 * TMDB's `media_type` identifiers - API values, never user-facing text.
 *
 * These appear in every part of the app that handles more than one kind of title: trending's tabs,
 * multi-search's results, and a person's combined credits. They were declared five separate times
 * (twice as `MEDIA_TYPE_*`, once as `API_VALUE_*`, and twice as bare literals) before this object,
 * which is exactly the drift the no-magic-strings rule exists to stop - one copy going out of step
 * with TMDB would break only the feature holding it.
 */
object MediaTypeConstant {
    const val MOVIE = "movie"
    const val TV = "tv"
    const val PERSON = "person"
}
