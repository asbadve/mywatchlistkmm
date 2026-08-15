package com.ajinkyabadve.kmmmywatchlist.features.auth.repository

import com.ajinkyabadve.kmmmywatchlist.features.auth.model.AccountDetails
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthRepository : AuthRepository {
    private val _sessionState = MutableStateFlow<UserSession?>(null)
    override val sessionState: StateFlow<UserSession?> = _sessionState.asStateFlow()

    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    var requestTokenToReturn: String = "fake_request_token"
    var shouldFailCreateRequestToken: Boolean = false
    var shouldFailCreateSession: Boolean = false

    override suspend fun createRequestToken(): String {
        if (shouldFailCreateRequestToken) {
            throw IllegalStateException("Fake error creating request token")
        }
        return requestTokenToReturn
    }

    override suspend fun createSession(requestToken: String): UserSession {
        if (shouldFailCreateSession) {
            throw IllegalStateException("Fake error creating session")
        }
        val session =
            UserSession(
                sessionId = "fake_session_123",
                accountId = 100L,
                username = "fakeuser",
                name = "Fake User",
                avatarUrl = "https://image.tmdb.org/t/p/w185/fake_avatar.jpg",
            )
        saveSession(session)
        return session
    }

    override suspend fun getAccountDetails(sessionId: String): AccountDetails =
        AccountDetails(
            id = 100L,
            name = "Fake User",
            username = "fakeuser",
        )

    override fun saveSession(session: UserSession) {
        _sessionState.value = session
    }

    override fun getUserSession(): UserSession? = _sessionState.value

    override fun clearSession() {
        _sessionState.value = null
    }

    override fun notifySessionExpired() {
        clearSession()
        _sessionExpiredEvent.tryEmit(Unit)
    }
}
