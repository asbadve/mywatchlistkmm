package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.auth.FakeWebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.auth.WebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeCustomListRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthScreenModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class MyFavScreenTabUiTest {
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeWebAuthLauncher: WebAuthLauncher

    @BeforeTest
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        fakeWebAuthLauncher = FakeWebAuthLauncher()
    }

    @Test
    fun testLoggedOutStateRendersLoginCard() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                MyFavScreenTab(
                    windowSize = WindowSize.COMPACT,
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                )
            }

            onNodeWithText("Sign in to TMDB").assertIsDisplayed()
            onNodeWithText("Log in with TMDB").assertIsDisplayed()
        }

    @Test
    fun testLoggedInStateRendersFavoritesWatchlistAndListsTabs() =
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
                    windowSize = WindowSize.COMPACT,
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                    listsRepository = FakeListsRepository(),
                    trackedMediaRepository = FakeTrackedMediaRepository(),
                    customListRepository = FakeCustomListRepository(),
                )
            }

            onNodeWithText("Favorites").assertIsDisplayed()
            onNodeWithText("Watchlist").assertIsDisplayed()
            onNodeWithText("Lists").assertIsDisplayed()
        }
}
