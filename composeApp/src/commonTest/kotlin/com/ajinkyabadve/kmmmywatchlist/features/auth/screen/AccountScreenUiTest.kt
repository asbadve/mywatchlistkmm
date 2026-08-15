package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.auth.FakeWebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.auth.WebAuthLauncher
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
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AccountScreenUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeWebAuthLauncher: WebAuthLauncher

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        fakeWebAuthLauncher = FakeWebAuthLauncher()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoggedOutFullScreenShowsBackArrowAndSignInCard() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                )
            }

            onNodeWithContentDescription("Back").assertIsDisplayed()
            onNodeWithText("Sign in to TMDB").assertIsDisplayed()
            onNodeWithText("Log in with TMDB").assertIsDisplayed()
        }

    @Test
    fun testLoggedOutDialogShowsCloseAndSignInCard() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = true,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                )
            }

            onNodeWithContentDescription("Close").assertIsDisplayed()
            onNodeWithText("Sign in to TMDB").assertIsDisplayed()
        }

    @Test
    fun testClickingLoginSignsInAndShowsProfile() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                )
            }

            onNodeWithText("Log in with TMDB").performClick()

            onNodeWithText("Welcome, Fake User!").assertIsDisplayed()
            onNodeWithText("@fakeuser").assertIsDisplayed()
        }

    @Test
    fun testLoggedInStateShowsProfileAndLogoutRow() =
        runComposeUiTest {
            val session =
                UserSession(
                    sessionId = "session_999",
                    accountId = 99L,
                    username = "jane_doe",
                    name = "Jane Doe",
                )
            fakeAuthRepository.saveSession(session)
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)

            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                )
            }

            onNodeWithText("Welcome, Jane Doe!").assertIsDisplayed()
            onNodeWithText("@jane_doe").assertIsDisplayed()
            onNodeWithText("Log out").assertIsDisplayed()
        }

    @Test
    fun testClickingLogoutRowLogsOutAndInvokesBackCallback() =
        runComposeUiTest {
            var backClicked = false
            val session =
                UserSession(
                    sessionId = "session_999",
                    accountId = 99L,
                    username = "jane_doe",
                    name = "Jane Doe",
                )
            fakeAuthRepository.saveSession(session)
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)

            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = { backClicked = true },
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                )
            }

            onNodeWithText("Log out").performClick()

            assertTrue(backClicked)
            onNodeWithText("Sign in to TMDB").assertIsDisplayed()
        }

    @Test
    fun testClickingBackInvokesCallback() =
        runComposeUiTest {
            var backClicked = false
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = { backClicked = true },
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                )
            }

            onNodeWithContentDescription("Back").performClick()

            assertTrue(backClicked)
        }
}
