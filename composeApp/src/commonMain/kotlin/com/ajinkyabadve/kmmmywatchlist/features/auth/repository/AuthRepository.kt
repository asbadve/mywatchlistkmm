package com.ajinkyabadve.kmmmywatchlist.features.auth.repository

import com.ajinkyabadve.kmmmywatchlist.createSettings
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.AccountDetails
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.CreateSessionRequest
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.RequestTokenResponse
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.SessionResponse
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.TmdbAvatar
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import com.ajinkyabadve.kmmmywatchlist.network.constant.NetworkConstant
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinproject.composeapp.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException

object AuthConstant {
    const val KEY_SESSION_ID = "auth_session_id"
    const val KEY_ACCOUNT_ID = "auth_account_id"
    const val KEY_USERNAME = "auth_username"
    const val KEY_NAME = "auth_name"
    const val KEY_AVATAR_URL = "auth_avatar_url"
    const val AUTH_CALLBACK_SCHEME = "mywatchlist"
    const val AUTH_CALLBACK_HOST = "auth-callback"
    const val AUTH_CALLBACK_URL = "$AUTH_CALLBACK_SCHEME://$AUTH_CALLBACK_HOST"
    const val TMDB_AUTH_BASE_URL = "https://www.themoviedb.org/authenticate/"
    const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185"

    // Most TMDB accounts never upload a native TMDB avatar, so tmdb.avatar_path is usually null -
    // gravatar.hash (derived from the account's email) is the fallback that actually has an image
    // most of the time. `d=identicon` gives a deterministic generated pattern instead of Gravatar's
    // default grey silhouette when the hash has no image registered, so this URL never 404s.
    const val GRAVATAR_BASE_URL = "https://www.gravatar.com/avatar/"
    const val GRAVATAR_QUERY = "?s=200&d=identicon"
}

/**
 * Prefers a TMDB-hosted avatar (the CDN path resolved against [AuthConstant.TMDB_IMAGE_BASE_URL]);
 * falls back to Gravatar (resolved from the account's email hash) when the user has never uploaded
 * one to TMDB; returns null only when neither source has anything to show.
 */
internal fun resolveAvatarUrl(avatar: TmdbAvatar): String? {
    val tmdbAvatarPath = avatar.tmdb.avatarPath
    val gravatarHash = avatar.gravatar.hash
    return when {
        !tmdbAvatarPath.isNullOrEmpty() -> "${AuthConstant.TMDB_IMAGE_BASE_URL}$tmdbAvatarPath"
        gravatarHash.isNotEmpty() -> "${AuthConstant.GRAVATAR_BASE_URL}$gravatarHash${AuthConstant.GRAVATAR_QUERY}"
        else -> null
    }
}

interface AuthRepository {
    val sessionState: StateFlow<UserSession?>
    val sessionExpiredEvent: SharedFlow<Unit>

    suspend fun createRequestToken(): String

    suspend fun createSession(requestToken: String): UserSession

    suspend fun getAccountDetails(sessionId: String): AccountDetails

    fun saveSession(session: UserSession)

    fun getUserSession(): UserSession?

    fun clearSession()

    fun notifySessionExpired()
}

class AuthRepositoryImpl(
    private val tmdbClient: TmdbClient = TmdbClient.TmdbApiClient.newInstance,
    private val settings: Settings = createSettings(),
) : AuthRepository {
    private val _sessionState = MutableStateFlow<UserSession?>(loadSessionFromSettings())
    override val sessionState: StateFlow<UserSession?> = _sessionState.asStateFlow()

    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    private fun loadSessionFromSettings(): UserSession? {
        val sessionId = settings.getString(AuthConstant.KEY_SESSION_ID, "")
        if (sessionId.isEmpty()) return null
        val accountId = settings.getLong(AuthConstant.KEY_ACCOUNT_ID, 0L)
        val username = settings.getString(AuthConstant.KEY_USERNAME, "")
        val name = settings.getString(AuthConstant.KEY_NAME, "")
        val avatarUrl = settings.getString(AuthConstant.KEY_AVATAR_URL, "").ifEmpty { null }
        return UserSession(
            sessionId = sessionId,
            accountId = accountId,
            username = username,
            name = name,
            avatarUrl = avatarUrl,
        )
    }

    override suspend fun createRequestToken(): String =
        try {
            val response: HttpResponse =
                tmdbClient.client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = NetworkConstant.HOST
                        trailingQuery = true
                        encodedPath = "/3/authentication/token/new"
                        parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    }
                }
            val body: RequestTokenResponse = response.body()
            if (!body.success || body.requestToken.isEmpty()) {
                throw IllegalStateException("Failed to create request token from TMDB")
            }
            body.requestToken
        } catch (e: HttpExceptions) {
            Napier.e(tag = TAG, throwable = e) { "Http error creating request token" }
            throw e
        } catch (e: IOException) {
            Napier.e(tag = TAG, throwable = e) { "Network I/O error creating request token" }
            throw e
        } catch (e: ContentConvertException) {
            Napier.e(tag = TAG, throwable = e) { "Payload parse error creating request token" }
            throw e
        } catch (e: SerializationException) {
            Napier.e(tag = TAG, throwable = e) { "Serialization error creating request token" }
            throw e
        } catch (e: HttpRequestTimeoutException) {
            Napier.e(tag = TAG, throwable = e) { "Timeout error creating request token" }
            throw e
        }

    override suspend fun createSession(requestToken: String): UserSession =
        try {
            val response: HttpResponse =
                tmdbClient.client.post {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = NetworkConstant.HOST
                        trailingQuery = true
                        encodedPath = "/3/authentication/session/new"
                        parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(CreateSessionRequest(requestToken = requestToken))
                }
            val sessionResponse: SessionResponse = response.body()
            if (!sessionResponse.success || sessionResponse.sessionId.isEmpty()) {
                throw IllegalStateException("Failed to create session with TMDB")
            }
            val accountDetails = getAccountDetails(sessionResponse.sessionId)
            val userSession =
                UserSession(
                    sessionId = sessionResponse.sessionId,
                    accountId = accountDetails.id,
                    username = accountDetails.username,
                    name = accountDetails.name,
                    avatarUrl = resolveAvatarUrl(accountDetails.avatar),
                )
            saveSession(userSession)
            userSession
        } catch (e: HttpExceptions) {
            Napier.e(tag = TAG, throwable = e) { "Http error creating session" }
            if (e.response.status.value == 401) {
                notifySessionExpired()
            }
            throw e
        } catch (e: IOException) {
            Napier.e(tag = TAG, throwable = e) { "Network I/O error creating session" }
            throw e
        } catch (e: ContentConvertException) {
            Napier.e(tag = TAG, throwable = e) { "Payload parse error creating session" }
            throw e
        } catch (e: SerializationException) {
            Napier.e(tag = TAG, throwable = e) { "Serialization error creating session" }
            throw e
        }

    override suspend fun getAccountDetails(sessionId: String): AccountDetails =
        try {
            val response: HttpResponse =
                tmdbClient.client.get {
                    url {
                        protocol = URLProtocol.HTTPS
                        host = NetworkConstant.HOST
                        trailingQuery = true
                        encodedPath = "/3/account"
                        parameters.append(NetworkConstant.API_KEY, BuildConfig.TMDB_API_KEY)
                        parameters.append("session_id", sessionId)
                    }
                }
            response.body()
        } catch (e: HttpExceptions) {
            Napier.e(tag = TAG, throwable = e) { "Http error getting account details" }
            if (e.response.status.value == 401) {
                notifySessionExpired()
            }
            throw e
        } catch (e: IOException) {
            Napier.e(tag = TAG, throwable = e) { "Network error getting account details" }
            throw e
        } catch (e: ContentConvertException) {
            Napier.e(tag = TAG, throwable = e) { "Payload parse error getting account details" }
            throw e
        } catch (e: SerializationException) {
            Napier.e(tag = TAG, throwable = e) { "Serialization error getting account details" }
            throw e
        }

    override fun saveSession(session: UserSession) {
        settings.putString(AuthConstant.KEY_SESSION_ID, session.sessionId)
        settings.putLong(AuthConstant.KEY_ACCOUNT_ID, session.accountId)
        settings.putString(AuthConstant.KEY_USERNAME, session.username)
        settings.putString(AuthConstant.KEY_NAME, session.name)
        settings.putString(AuthConstant.KEY_AVATAR_URL, session.avatarUrl.orEmpty())
        _sessionState.value = session
    }

    override fun getUserSession(): UserSession? = _sessionState.value

    override fun clearSession() {
        settings.remove(AuthConstant.KEY_SESSION_ID)
        settings.remove(AuthConstant.KEY_ACCOUNT_ID)
        settings.remove(AuthConstant.KEY_USERNAME)
        settings.remove(AuthConstant.KEY_NAME)
        settings.remove(AuthConstant.KEY_AVATAR_URL)
        _sessionState.value = null
    }

    override fun notifySessionExpired() {
        clearSession()
        _sessionExpiredEvent.tryEmit(Unit)
    }

    private companion object {
        const val TAG = "AuthRepositoryImpl"
    }
}
