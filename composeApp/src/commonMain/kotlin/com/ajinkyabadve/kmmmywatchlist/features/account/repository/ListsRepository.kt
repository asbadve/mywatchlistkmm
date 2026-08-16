package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import com.ajinkyabadve.kmmmywatchlist.features.account.model.CreateListRequest
import com.ajinkyabadve.kmmmywatchlist.features.account.model.CreateListResponse
import com.ajinkyabadve.kmmmywatchlist.features.account.model.MediaIdRequest
import com.ajinkyabadve.kmmmywatchlist.features.account.model.StatusResponse
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListPageResult
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import io.ktor.client.call.body
import io.ktor.client.request.delete
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
 * Custom lists via TMDB's v3 `/3/list` API, authenticated with the same session id
 * [com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository] already holds - no
 * separate auth needed. **Movie-only**: `POST /3/list/{id}/add_item` (confirmed against the live
 * OpenAPI spec, operationId `list-add-movie`) only ever accepts a movie id, and there is no v3
 * equivalent for TV. Mixed movie/TV lists exist only in TMDB's v4 API, which needs a wholly
 * separate OAuth-style access-token flow not currently in the published API surface at all (zero
 * `/4/...` paths in `openapi/tmdb-api.json` as of this writing) - deferred, see
 * future_features_checklist.md.
 */
interface ListsRepository {
    suspend fun getLists(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): TmdbListPageResult

    suspend fun getListDetails(
        listId: Long,
        sessionId: String,
    ): TmdbListDetail

    /** Returns the new list's id. */
    suspend fun createList(
        sessionId: String,
        name: String,
        description: String,
    ): Long

    suspend fun addMovieToList(
        listId: Long,
        sessionId: String,
        movieId: Long,
    ): StatusResponse

    suspend fun removeMovieFromList(
        listId: Long,
        sessionId: String,
        movieId: Long,
    ): StatusResponse

    suspend fun deleteList(
        listId: Long,
        sessionId: String,
    ): StatusResponse
}

class ListsRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
) : ListsRepository {
    override suspend fun getLists(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): TmdbListPageResult {
        val response: HttpResponse =
            tmdbClient.client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = "$ACCOUNT_PATH$accountId$LISTS"
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.SESSION_ID, sessionId)
                    parameters.append(NetworkConstant.PAGE, page.toString())
                }
            }
        return response.body()
    }

    // `session_id` isn't in TMDB's own OpenAPI spec for this endpoint (it documents this as public
    // data), but a private list (the default for a newly created list) 401s without it - confirmed
    // by curling a real private list both with and without the param. Sent unconditionally since a
    // public list ignores the extra param harmlessly.
    override suspend fun getListDetails(
        listId: Long,
        sessionId: String,
    ): TmdbListDetail {
        val response: HttpResponse =
            tmdbClient.client.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = "$LIST$listId"
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.SESSION_ID, sessionId)
                }
            }
        return response.body()
    }

    override suspend fun createList(
        sessionId: String,
        name: String,
        description: String,
    ): Long {
        val response: HttpResponse =
            tmdbClient.client.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = LIST_ROOT
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.SESSION_ID, sessionId)
                }
                contentType(ContentType.Application.Json)
                setBody(CreateListRequest(name = name, description = description))
            }
        val body: CreateListResponse = response.body()
        return body.listId
    }

    override suspend fun addMovieToList(
        listId: Long,
        sessionId: String,
        movieId: Long,
    ): StatusResponse {
        val response: HttpResponse =
            tmdbClient.client.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = "$LIST$listId$ADD_ITEM"
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.SESSION_ID, sessionId)
                }
                contentType(ContentType.Application.Json)
                setBody(MediaIdRequest(mediaId = movieId))
            }
        return response.body()
    }

    override suspend fun removeMovieFromList(
        listId: Long,
        sessionId: String,
        movieId: Long,
    ): StatusResponse {
        val response: HttpResponse =
            tmdbClient.client.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = "$LIST$listId$REMOVE_ITEM"
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.SESSION_ID, sessionId)
                }
                contentType(ContentType.Application.Json)
                setBody(MediaIdRequest(mediaId = movieId))
            }
        return response.body()
    }

    override suspend fun deleteList(
        listId: Long,
        sessionId: String,
    ): StatusResponse {
        val response: HttpResponse =
            tmdbClient.client.delete {
                url {
                    protocol = URLProtocol.HTTPS
                    host = NetworkConstant.HOST
                    trailingQuery = true
                    encodedPath = "$LIST$listId"
                    parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    parameters.append(NetworkConstant.SESSION_ID, sessionId)
                }
            }
        return response.body()
    }

    private companion object {
        const val ACCOUNT_PATH = "/3/account/"
        const val LISTS = "/lists"
        const val LIST_ROOT = "/3/list"
        const val LIST = "/3/list/"
        const val ADD_ITEM = "/add_item"
        const val REMOVE_ITEM = "/remove_item"
    }
}
