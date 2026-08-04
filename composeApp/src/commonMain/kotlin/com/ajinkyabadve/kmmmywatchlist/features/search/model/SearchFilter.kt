package com.ajinkyabadve.kmmmywatchlist.features.search.model

import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.filter_all
import mywatchlist.composeapp.generated.resources.filter_movies
import mywatchlist.composeapp.generated.resources.filter_people
import mywatchlist.composeapp.generated.resources.filter_tv_shows
import org.jetbrains.compose.resources.StringResource

/**
 * The chip row above the search results. Filtering is client-side over what's already been loaded:
 * `/3/search/multi` returns all three media types interleaved by relevance in one page and has no
 * server-side type parameter, so narrowing to a single type here never costs an extra request.
 */
enum class SearchFilter(
    val label: StringResource,
    val mediaType: SearchMediaType?,
) {
    ALL(Res.string.filter_all, null),
    MOVIES(Res.string.filter_movies, SearchMediaType.MOVIE),
    TV_SHOWS(Res.string.filter_tv_shows, SearchMediaType.TV),
    PEOPLE(Res.string.filter_people, SearchMediaType.PERSON),
    ;

    fun matches(type: SearchMediaType?): Boolean = mediaType == null || mediaType == type
}
