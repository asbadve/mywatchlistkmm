package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

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
class MyFavScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var screenModel: MyFavScreenModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        screenModel = MyFavScreenModel(authRepository = fakeAuthRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsLoggedOut() {
        assertTrue(screenModel.uiState.value is MyFavUiState.LoggedOut)
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

        val modelWithSession = MyFavScreenModel(authRepository = fakeAuthRepository)
        val state = modelWithSession.uiState.value
        assertTrue(state is MyFavUiState.LoggedIn)
        assertEquals("existinguser", state.session.username)
    }

    @Test
    fun testCompleteSessionCreationSuccess() {
        screenModel.completeSessionCreation("valid_token")
        val state = screenModel.uiState.value
        assertTrue(state is MyFavUiState.LoggedIn)
        assertEquals("fakeuser", state.session.username)
    }

    @Test
    fun testLogoutClickedResetsToLoggedOut() {
        screenModel.completeSessionCreation("valid_token")
        assertTrue(screenModel.uiState.value is MyFavUiState.LoggedIn)

        screenModel.onLogoutClicked()
        assertTrue(screenModel.uiState.value is MyFavUiState.LoggedOut)
    }
}
