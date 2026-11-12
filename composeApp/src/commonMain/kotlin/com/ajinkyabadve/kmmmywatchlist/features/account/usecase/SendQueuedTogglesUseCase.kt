package com.ajinkyabadve.kmmmywatchlist.features.account.usecase

import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl

/**
 * Sends the signed-in account's offline favorite/watchlist changes to TMDB - the one body every
 * automatic trigger runs (app start, Android's network-constrained background job; see
 * `QueuedToggleSyncScheduler`). Signed out, there is no session to send with, so it does nothing.
 */
class SendQueuedTogglesUseCase(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val trackedMediaRepository: TrackedMediaRepository = TrackedMediaRepositoryImpl(),
) {
    /** True when nothing is left queued (or nobody is signed in), false when changes are still waiting. */
    suspend operator fun invoke(): Boolean {
        val session = authRepository.getUserSession() ?: return true
        return trackedMediaRepository.sendQueuedToggles(session.accountId, session.sessionId)
    }
}
