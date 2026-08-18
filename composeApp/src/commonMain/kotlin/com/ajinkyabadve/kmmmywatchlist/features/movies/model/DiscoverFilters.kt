package com.ajinkyabadve.kmmmywatchlist.features.movies.model

import kotlinx.serialization.Serializable

/**
 * A pending or applied Discover filter set. Built client-side and turned into TMDB's discover
 * query params by `DiscoverRepository` - never sent to/received from TMDB directly, so this is a
 * plain `@Serializable` (for `DiscoverFilterRepository`'s settings cache), not a network model.
 *
 * Keywords are stored as full [Keyword] (id + name), not just ids: TMDB's `with_keywords` param
 * only needs the id, but the filter dialog also has to *display* which keywords are currently
 * applied (as removable chips) when it's reopened - an id alone can't be labeled without an extra
 * lookup call, so the name travels with it from the moment it's picked in the search suggestions.
 */
@Serializable
data class DiscoverFilters(
    val genreIds: Set<Int> = emptySet(),
    val keywords: List<Keyword> = emptyList(),
    val year: Int? = null,
    val sortBy: String = DiscoverConstant.DEFAULT_SORT_BY,
)

object DiscoverConstant {
    const val DEFAULT_SORT_BY = "popularity.desc"

    /** `sort_by` values TMDB's `/3/discover/movie` accepts (includes revenue, unlike TV). */
    val MOVIE_SORT_OPTIONS =
        listOf(
            "popularity.desc",
            "popularity.asc",
            "vote_average.desc",
            "vote_average.asc",
            "vote_count.desc",
            "vote_count.asc",
            "primary_release_date.desc",
            "primary_release_date.asc",
            "revenue.desc",
            "revenue.asc",
            "title.asc",
            "title.desc",
        )

    /** `sort_by` values TMDB's `/3/discover/tv` accepts. */
    val TV_SORT_OPTIONS =
        listOf(
            "popularity.desc",
            "popularity.asc",
            "vote_average.desc",
            "vote_average.asc",
            "vote_count.desc",
            "vote_count.asc",
            "first_air_date.desc",
            "first_air_date.asc",
            "name.asc",
            "name.desc",
        )
}
