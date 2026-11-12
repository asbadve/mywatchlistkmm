package com.ajinkyabadve.kmmmywatchlist.features.account.usecase

import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SendQueuedTogglesUseCaseTest {
    private val authRepository = FakeAuthRepository()
    private val trackedMediaRepository = FakeTrackedMediaRepository()
    private val sendQueuedToggles = SendQueuedTogglesUseCase(authRepository, trackedMediaRepository)

    @Test
    fun testSendsWithTheSignedInSession() =
        runTest {
            authRepository.saveSession(SESSION)

            assertTrue(sendQueuedToggles())

            assertEquals(listOf(ACCOUNT_ID to SESSION_ID), trackedMediaRepository.sendQueuedTogglesCalls)
        }

    /** Changes still waiting (offline) - the Android background job reads false as "retry". */
    @Test
    fun testReportsChangesStillWaiting() =
        runTest {
            authRepository.saveSession(SESSION)
            trackedMediaRepository.sendQueuedTogglesResult = false

            assertFalse(sendQueuedToggles())
        }

    /** Signed out: no session to send with, nothing to retry. */
    @Test
    fun testSignedOutSendsNothing() =
        runTest {
            assertTrue(sendQueuedToggles())

            assertTrue(trackedMediaRepository.sendQueuedTogglesCalls.isEmpty())
        }

    private companion object {
        const val ACCOUNT_ID = 1L
        const val SESSION_ID = "session"
        val SESSION = UserSession(sessionId = SESSION_ID, accountId = ACCOUNT_ID, username = "user", name = "User")
    }
}
