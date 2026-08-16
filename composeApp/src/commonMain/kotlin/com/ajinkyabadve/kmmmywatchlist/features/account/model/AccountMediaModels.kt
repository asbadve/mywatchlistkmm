package com.ajinkyabadve.kmmmywatchlist.features.account.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Favorite/watchlist GET responses (`/3/account/{id}/favorite/movies`, `/watchlist/tv`, ...)
// deserialize directly as `features.search.model.SearchPageResult`/`SearchResultItem` - TMDB
// returns the identical movie/TV item shape `/3/search/multi` does, just scoped to one media type
// per call instead of mixed - so the same heterogeneous-shape model already built for Search
// covers this without a new one.

/** One row of `GET /3/account/{account_id}/lists` - a custom list's summary, not its items. */
@Serializable
data class TmdbList(
    @SerialName("id") val id: Long = -1,
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable
data class TmdbListPageResult(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val list: List<TmdbList>? = null,
    @SerialName("total_results") val totalResults: Int? = null,
    @SerialName("total_pages") val totalPages: Int? = null,
)

/**
 * `GET /3/list/{list_id}`. Unlike [TmdbList]'s numeric `id`, this endpoint quotes it as a string -
 * not modeled here since callers already have the list id from navigation. `items` are always
 * movie-shaped: TMDB's v3 list API is movie-only (see AuthRepository/ListsRepository kdoc).
 */
@Serializable
data class TmdbListDetail(
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("items") val items: List<Movie> = emptyList(),
)

@Serializable
data class CreateListRequest(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("language") val language: String = "en",
)

@Serializable
data class CreateListResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("status_message") val statusMessage: String = "",
    @SerialName("list_id") val listId: Long = -1,
)

@Serializable
data class MediaIdRequest(
    @SerialName("media_id") val mediaId: Long,
)

@Serializable
data class SetFavoriteRequest(
    @SerialName("media_type") val mediaType: String,
    @SerialName("media_id") val mediaId: Long,
    @SerialName("favorite") val favorite: Boolean,
)

@Serializable
data class SetWatchlistRequest(
    @SerialName("media_type") val mediaType: String,
    @SerialName("media_id") val mediaId: Long,
    @SerialName("watchlist") val watchlist: Boolean,
)

/** Shared by every write endpoint that only ever answers with a status code/message. */
@Serializable
data class StatusResponse(
    @SerialName("status_code") val statusCode: Int = 0,
    @SerialName("status_message") val statusMessage: String = "",
)

/**
 * `GET /3/movie/{id}/account_states` / `GET /3/tv/{id}/account_states` - whether the signed-in
 * session (`session_id` query param, same auth as the favorite/watchlist writes above) already has
 * this title favorited or on its watchlist. `rated` is TMDB's per-title rating state, either `false`
 * or `{"value": n}`; this app doesn't surface ratings yet, so it's left unmodeled.
 */
@Serializable
data class AccountStates(
    @SerialName("favorite") val favorite: Boolean = false,
    @SerialName("watchlist") val watchlist: Boolean = false,
)
