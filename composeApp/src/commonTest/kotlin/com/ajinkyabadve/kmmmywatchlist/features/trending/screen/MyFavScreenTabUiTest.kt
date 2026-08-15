package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.auth.FakeWebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.auth.WebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class MyFavScreenTabUiTest {
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
    fun testLoggedOutStateRendersLoginCard() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                MyFavScreenTab(
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                )
            }

            onNodeWithText("Sign in to TMDB").assertIsDisplayed()
            onNodeWithText("Log in with TMDB").assertIsDisplayed()
        }

    @Test
    fun testLoggedInStateRendersProfileCard() =
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
                MyFavScreenTab(
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                )
            }

            onNodeWithText("Welcome, Jane Doe!").assertIsDisplayed()
            onNodeWithText("Your favorites and watchlist are coming soon.").assertIsDisplayed()
        }
}
