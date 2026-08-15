package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AccountScreenUiTest {
    private val session =
        UserSession(
            sessionId = "session_1",
            accountId = 1L,
            username = "jane_doe",
            name = "Jane Doe",
        )

    @Test
    fun testFullScreenPresentationShowsBackArrowAndProfile() =
        runComposeUiTest {
            setContent {
                AccountScreen(
                    session = session,
                    isDialogPresentation = false,
                    onBackClicked = {},
                    onLogoutClicked = {},
                )
            }

            onNodeWithContentDescription("Back").assertIsDisplayed()
            onNodeWithText("Welcome, Jane Doe!").assertIsDisplayed()
            onNodeWithText("@jane_doe").assertIsDisplayed()
            onNodeWithText("Log out").assertIsDisplayed()
        }

    @Test
    fun testDialogPresentationShowsCloseInsteadOfBack() =
        runComposeUiTest {
            setContent {
                AccountScreen(
                    session = session,
                    isDialogPresentation = true,
                    onBackClicked = {},
                    onLogoutClicked = {},
                )
            }

            onNodeWithContentDescription("Close").assertIsDisplayed()
            onNodeWithText("Welcome, Jane Doe!").assertIsDisplayed()
        }

    @Test
    fun testClickingLogoutRowInvokesCallback() =
        runComposeUiTest {
            var logoutClicked = false
            setContent {
                AccountScreen(
                    session = session,
                    isDialogPresentation = false,
                    onBackClicked = {},
                    onLogoutClicked = { logoutClicked = true },
                )
            }

            onNodeWithText("Log out").performClick()

            assertTrue(logoutClicked)
        }

    @Test
    fun testClickingBackInvokesCallback() =
        runComposeUiTest {
            var backClicked = false
            setContent {
                AccountScreen(
                    session = session,
                    isDialogPresentation = false,
                    onBackClicked = { backClicked = true },
                    onLogoutClicked = {},
                )
            }

            onNodeWithContentDescription("Back").performClick()

            assertTrue(backClicked)
        }
}
