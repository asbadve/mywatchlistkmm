package com.ajinkyabadve.kmmmywatchlist.features.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestTokenResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("request_token") val requestToken: String = "",
)

@Serializable
data class CreateSessionRequest(
    @SerialName("request_token") val requestToken: String,
)

@Serializable
data class SessionResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("session_id") val sessionId: String = "",
)

@Serializable
data class TmdbAvatar(
    @SerialName("gravatar") val gravatar: TmdbGravatar = TmdbGravatar(),
    @SerialName("tmdb") val tmdb: TmdbAvatarPath = TmdbAvatarPath(),
)

@Serializable
data class TmdbGravatar(
    @SerialName("hash") val hash: String = "",
)

@Serializable
data class TmdbAvatarPath(
    @SerialName("avatar_path") val avatarPath: String? = null,
)

@Serializable
data class AccountDetails(
    @SerialName("id") val id: Long = 0L,
    @SerialName("name") val name: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("avatar") val avatar: TmdbAvatar = TmdbAvatar(),
)

@Serializable
data class UserSession(
    val sessionId: String,
    val accountId: Long,
    val username: String,
    val name: String,
    // A fully resolved, absolute image URL (TMDB CDN or Gravatar) - see
    // AuthRepositoryImpl.resolveAvatarUrl - not a TMDB-relative path, despite most other
    // TMDB *_path fields in this codebase being relative.
    val avatarUrl: String? = null,
)
