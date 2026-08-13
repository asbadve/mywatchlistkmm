package com.ajinkyabadve.kmmmywatchlist.features.person

/**
 * TMDB `media_type` values the person tests build credits with.
 *
 * Shared because the distinction is the point of more than one of them: a person's TV credits carry
 * the *series* first-air date, so mixing the two up is exactly the bug
 * `PersonHeroBackdropTest.testFirstFilmYearIgnoresTheAirDateOfLongRunningShows` guards against.
 *
 * Deliberately a second declaration rather than an import of `MediaTypeConstant`: these are the raw
 * wire values TMDB sends, which is what the credit accessors are being verified against - a test
 * that imported the constant under test would keep passing if that constant changed. Same reasoning
 * as `TrendingScreenTabViewModelTest`, which states its own copies for the same reason.
 */
object PersonTestConstant {
    const val MEDIA_TYPE_MOVIE = "movie"
    const val MEDIA_TYPE_TV = "tv"
}
