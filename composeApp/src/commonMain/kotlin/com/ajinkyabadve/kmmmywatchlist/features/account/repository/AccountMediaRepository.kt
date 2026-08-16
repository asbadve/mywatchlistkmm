package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.model.AccountStates
import com.ajinkyabadve.kmmmywatchlist.features.account.model.SetFavoriteRequest
import com.ajinkyabadve.kmmmywatchlist.features.account.model.SetWatchlistRequest
import com.ajinkyabadve.kmmmywatchlist.features.account.model.StatusResponse
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import kotlinproject.composeapp.BuildConfig

/**
 * A signed-in user's favorite and watchlist media (movies + TV both, per TMDB's
 * `POST /3/account/{account_id}/favorite`/`watchlist` - confirmed against the live OpenAPI spec
 * to take `media_type: "movie"|"tv"`, unlike the v3 custom-list API `ListsRepository` covers,
 * which is movie-only). GET responses reuse [SearchPageResult] - see its file kdoc.
 *
 * [getAccountStates] is the per-title counterpart - `GET /3/movie/{id}/account_states` or
 * `/3/tv/{id}/account_states` - used to pre-check a single title's favorite/watchlist icons on the
 * detail screen instead of always starting them unset.
 */
interface AccountMediaRepository {
    suspend fun getFavoriteMovies(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult

    suspend fun getFavoriteTv(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult

    suspend fun getWatchlistMovies(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult

    suspend fun getWatchlistTv(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult

    suspend fun setFavorite(
        accountId: Long,
        sessionId: String,
        mediaType: String,
        mediaId: Long,
        favorite: Boolean,
    ): StatusResponse

    suspend fun setWatchlist(
        accountId: Long,
        sessionId: String,
        mediaType: String,
        mediaId: Long,
        watchlist: Boolean,
    ): StatusResponse

    suspend fun getAccountStates(
        sessionId: String,
        mediaType: String,
        mediaId: Long,
    ): AccountStates
}

class AccountMediaRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : AccountMediaRepository {
    override suspend fun getFavoriteMovies(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult = getAccountMediaPage("$ACCOUNT_PATH$accountId$FAVORITE_MOVIES", sessionId, page)

    override suspend fun getFavoriteTv(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult = getAccountMediaPage("$ACCOUNT_PATH$accountId$FAVORITE_TV", sessionId, page)

    override suspend fun getWatchlistMovies(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult = getAccountMediaPage("$ACCOUNT_PATH$accountId$WATCHLIST_MOVIES", sessionId, page)

    override suspend fun getWatchlistTv(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult = getAccountMediaPage("$ACCOUNT_PATH$accountId$WATCHLIST_TV", sessionId, page)

    private suspend fun getAccountMediaPage(
        path: String,
        sessionId: String,
        page: Int,
    ): SearchPageResult {
        val response: HttpResponse =
            tmdbClient.client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = path
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.SESSION_ID, sessionId)
                    parameters.append(NetworkConstant.PAGE, page.toString())
                }
            }
        return response.body()
    }

    override suspend fun setFavorite(
        accountId: Long,
        sessionId: String,
        mediaType: String,
        mediaId: Long,
        favorite: Boolean,
    ): StatusResponse {
        val response: HttpResponse =
            tmdbClient.client.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = "$ACCOUNT_PATH$accountId$FAVORITE"
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.SESSION_ID, sessionId)
                }
                contentType(ContentType.Application.Json)
                setBody(SetFavoriteRequest(mediaType = mediaType, mediaId = mediaId, favorite = favorite))
            }
        return response.body()
    }

    override suspend fun setWatchlist(
        accountId: Long,
        sessionId: String,
        mediaType: String,
        mediaId: Long,
        watchlist: Boolean,
    ): StatusResponse {
        val response: HttpResponse =
            tmdbClient.client.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = "$ACCOUNT_PATH$accountId$WATCHLIST"
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.SESSION_ID, sessionId)
                }
                contentType(ContentType.Application.Json)
                setBody(SetWatchlistRequest(mediaType = mediaType, mediaId = mediaId, watchlist = watchlist))
            }
        return response.body()
    }

    override suspend fun getAccountStates(
        sessionId: String,
        mediaType: String,
        mediaId: Long,
    ): AccountStates {
        val mediaPath = if (mediaType == MediaTypeConstant.TV) TV else MOVIE
        val response: HttpResponse =
            tmdbClient.client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = "$mediaPath$mediaId$ACCOUNT_STATES"
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.SESSION_ID, sessionId)
                }
            }
        return response.body()
    }

    private companion object {
        const val ACCOUNT_PATH = "/3/account/"
        const val FAVORITE_MOVIES = "/favorite/movies"
        const val FAVORITE_TV = "/favorite/tv"
        const val WATCHLIST_MOVIES = "/watchlist/movies"
        const val WATCHLIST_TV = "/watchlist/tv"
        const val FAVORITE = "/favorite"
        const val WATCHLIST = "/watchlist"
        const val MOVIE = "/3/movie/"
        const val TV = "/3/tv/"
        const val ACCOUNT_STATES = "/account_states"
    }
}
