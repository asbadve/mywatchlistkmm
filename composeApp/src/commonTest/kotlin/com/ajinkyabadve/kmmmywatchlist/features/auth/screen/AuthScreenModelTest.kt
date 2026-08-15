package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var screenModel: AuthScreenModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsLoggedOut() {
        assertTrue(screenModel.uiState.value is AuthUiState.LoggedOut)
    }

    @Test
    fun testInitialStateIsLoggedInWhenSessionExists() {
        val existingSession =
            UserSession(
                sessionId = "existing_session",
                accountId = 55L,
                username = "existinguser",
                name = "Existing User",
            )
        fakeAuthRepository.saveSession(existingSession)

        val modelWithSession = AuthScreenModel(authRepository = fakeAuthRepository)
        val state = modelWithSession.uiState.value
        assertTrue(state is AuthUiState.LoggedIn)
        assertEquals("existinguser", state.session.username)
    }

    @Test
    fun testCompleteSessionCreationSuccess() {
        screenModel.completeSessionCreation("valid_token")
        val state = screenModel.uiState.value
        assertTrue(state is AuthUiState.LoggedIn)
        assertEquals("fakeuser", state.session.username)
    }

    @Test
    fun testLogoutClickedResetsToLoggedOut() {
        screenModel.completeSessionCreation("valid_token")
        assertTrue(screenModel.uiState.value is AuthUiState.LoggedIn)

        screenModel.onLogoutClicked()
        assertTrue(screenModel.uiState.value is AuthUiState.LoggedOut)
    }
}
