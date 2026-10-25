package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.auth.FakeWebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.auth.WebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.file.FakeBackupFileLauncher
import com.ajinkyabadve.kmmmywatchlist.core.notification.FakeNotificationPermissionRequester
import com.ajinkyabadve.kmmmywatchlist.core.notification.LocalNotificationPermissionRequester
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.backup.screen.BackupTestFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AccountScreenUiTest {
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeWebAuthLauncher: WebAuthLauncher
    private lateinit var fixture: AccountTestFixture

    @BeforeTest
    fun setUp() {
        fakeAuthRepository = FakeAuthRepository()
        fakeWebAuthLauncher = FakeWebAuthLauncher()
        fixture = AccountTestFixture()
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
                    accountScreenModel = fixture.screenModel,
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
                    accountScreenModel = fixture.screenModel,
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
                    accountScreenModel = fixture.screenModel,
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
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithText("Welcome, Jane Doe!").assertIsDisplayed()
            onNodeWithText("@jane_doe").assertIsDisplayed()
            onNodeWithText("Region").assertIsDisplayed()
            onNodeWithText("Default fallback region").assertIsDisplayed()
            onNodeWithText("Used for watch provider availability only").assertIsDisplayed()
            onNodeWithText("Restricted Mode").assertIsDisplayed()
            onNodeWithText("Episode notifications").assertIsDisplayed()
            // The debug section grew a third row ("Poll collection notifications now"), pushing
            // "Log out" below the initial scroll viewport - scroll it into view before asserting.
            onNodeWithText("Log out").performScrollTo().assertIsDisplayed()
        }

    @Test
    fun testTogglingRestrictedModePersistsTheChange() =
        runComposeUiTest {
            val session =
                UserSession(sessionId = "session_999", accountId = 99L, username = "jane_doe", name = "Jane Doe")
            fakeAuthRepository.saveSession(session)
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            fixture = AccountTestFixture(restrictedModeEnabled = true)

            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            onAllNodes(isToggleable())[0].performClick()

            assertTrue(fixture.restrictedMode.setRestrictedModeCalls.contains(false))
        }

    @Test
    fun testTogglingEpisodeNotificationsOnRequestsPermissionAndPersists() =
        runComposeUiTest {
            val session =
                UserSession(sessionId = "session_999", accountId = 99L, username = "jane_doe", name = "Jane Doe")
            fakeAuthRepository.saveSession(session)
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)

            setContent {
                CompositionLocalProvider(LocalNotificationPermissionRequester provides FakeNotificationPermissionRequester()) {
                    AccountScreen(
                        isDialogPresentation = false,
                        onBackClicked = {},
                        webAuthLauncher = fakeWebAuthLauncher,
                        accountScreenModel = fixture.screenModel,
                        screenModel = screenModel,
                    )
                }
            }

            onAllNodes(isToggleable())[1].performClick()
            waitForIdle()

            assertTrue(fixture.notificationSettings.setEpisodeNotificationsEnabledCalls.contains(true))
        }

    @Test
    fun testDebugPollNowRowIsShown() =
        runComposeUiTest {
            // isDebugBuild() is unconditionally true on desktop (no separate release pipeline
            // exists for this platform yet - see PlatformUtil.kt), so the row is always present here.
            val session =
                UserSession(sessionId = "session_999", accountId = 99L, username = "jane_doe", name = "Jane Doe")
            fakeAuthRepository.saveSession(session)
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)

            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithText("Poll episode notifications now").assertIsDisplayed()
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
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            // Same scroll-into-view need as testLoggedInStateShowsProfileAndLogoutRow above - the
            // debug section's third row pushes "Log out" below the initial scroll viewport.
            onNodeWithText("Log out").performScrollTo().performClick()

            assertTrue(backClicked)
            // The list is still scrolled down to where "Log out" was; the sign-in prompt is at the top.
            onNodeWithText("Sign in to TMDB").performScrollTo().assertIsDisplayed()
        }

    @Test
    fun testLoggedOutStateAlsoShowsRegionAndRestrictedModeSettings() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            // Region/restricted-mode use TMDB's API-key-only endpoints, so they shouldn't require
            // being signed in - only "Log out" (which needs a session) is gated behind LoggedIn.
            onNodeWithText("Region").assertIsDisplayed()
            onNodeWithText("Default fallback region").assertIsDisplayed()
            onNodeWithText("Restricted Mode").assertIsDisplayed()
            onNodeWithText("Log out").assertDoesNotExist()
        }

    @Test
    fun testErrorStateAlsoShowsRegionAndRestrictedModeSettings() =
        runComposeUiTest {
            val deniedWebAuthLauncher =
                object : WebAuthLauncher {
                    override fun launchAuth(
                        authUrl: String,
                        redirectScheme: String,
                        onResult: (requestToken: String?, approved: Boolean) -> Unit,
                    ) {
                        onResult(null, false)
                    }
                }
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = deniedWebAuthLauncher,
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithText("Log in with TMDB").performClick()

            onNodeWithText("Authentication was cancelled or failed. Please try again.").assertIsDisplayed()
            onNodeWithText("Region").assertIsDisplayed()
            onNodeWithText("Restricted Mode").assertIsDisplayed()
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
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithContentDescription("Back").performClick()

            assertTrue(backClicked)
        }

    @Test
    fun testReleaseReminderRowsShowTheDefaultTimeOnMobile() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithText(RELEASE_REMINDERS_LABEL).performScrollTo().assertIsDisplayed()
            onNodeWithText(REMINDER_TIME_LABEL).performScrollTo().assertIsDisplayed()
            onNodeWithText(DEFAULT_REMINDER_TIME).performScrollTo().assertIsDisplayed()
        }

    @Test
    fun testReleaseReminderDebugRowsAreShownInDebugBuilds() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            // isDebugBuild() is always true on desktop, which is where this test runs.
            listOf(DEBUG_FIRE_TEST_REMINDER, DEBUG_POLL_RELEASE_REMINDERS, DEBUG_SHOW_PENDING, DEBUG_CLEAR_REMINDERS).forEach { label ->
                onNodeWithText(label).performScrollTo().assertIsDisplayed()
            }
        }

    @Test
    fun testClearAllRemindersDebugRowDeletesSavedReminders() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithText(DEBUG_CLEAR_REMINDERS).performScrollTo().performClick()
            waitUntil { fixture.reminders.deleteAllCallCount == 1 }

            assertEquals(1, fixture.reminders.deleteAllCallCount)
        }

    @Test
    fun testReleaseReminderRowsAreHiddenWhereRemindersCantFire() =
        runComposeUiTest {
            fixture = AccountTestFixture(releaseRemindersSupported = false)
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithText(REMINDER_TIME_LABEL).assertDoesNotExist()
            onNodeWithText(DEBUG_FIRE_TEST_REMINDER).assertDoesNotExist()
        }

    @Test
    fun testBackupRowOpensTheBackupDialog() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    backupFileLauncher = FakeBackupFileLauncher(),
                    backupScreenModel = BackupTestFixture().screenModel,
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithText(BACKUP_ROW_LABEL).performScrollTo().performClick()

            onNodeWithText(BACKUP_EXPORT_ACTION).assertIsDisplayed()
        }

    @Test
    fun testBackupRowIsHiddenWhereBackupIsUnsupported() =
        runComposeUiTest {
            fixture = AccountTestFixture(backupSupported = false)
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    backupFileLauncher = FakeBackupFileLauncher(),
                    backupScreenModel = BackupTestFixture().screenModel,
                    accountScreenModel = fixture.screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithText(BACKUP_ROW_LABEL).assertDoesNotExist()
        }

    @Test
    fun testDebugPollEpisodeRowRunsTheEpisodePoll() =
        runComposeUiTest {
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    accountScreenModel = fixture.screenModel,
                    screenModel = AuthScreenModel(authRepository = fakeAuthRepository),
                )
            }

            onNodeWithText(DEBUG_POLL_EPISODES).performScrollTo().performClick()
            waitForIdle()

            assertEquals(1, fixture.debugActions.episodePollCount)
        }

    @Test
    fun testShowPendingRemindersOpensAndClosesTheDialog() =
        runComposeUiTest {
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    accountScreenModel = fixture.screenModel,
                    screenModel = AuthScreenModel(authRepository = fakeAuthRepository),
                )
            }

            onNodeWithText(DEBUG_SHOW_PENDING).performScrollTo().performClick()
            waitUntil { fixture.screenModel.uiState.value.pendingReminders != null }

            onNodeWithText(PENDING_REMINDERS_EMPTY).assertIsDisplayed()

            onNodeWithText(CLOSE_ACTION).performClick()

            onNodeWithText(PENDING_REMINDERS_EMPTY).assertDoesNotExist()
        }

    @Test
    fun testFireTestReminderShowsWhenItWillFire() =
        runComposeUiTest {
            setContent {
                CompositionLocalProvider(LocalNotificationPermissionRequester provides FakeNotificationPermissionRequester()) {
                    AccountScreen(
                        isDialogPresentation = false,
                        onBackClicked = {},
                        webAuthLauncher = fakeWebAuthLauncher,
                        accountScreenModel = fixture.screenModel,
                        screenModel = AuthScreenModel(authRepository = fakeAuthRepository),
                    )
                }
            }

            onNodeWithText(DEBUG_FIRE_TEST_REMINDER).performScrollTo().performClick()
            waitUntil { fixture.screenModel.uiState.value.testReminderMessage != null }

            onNodeWithText(TEST_REMINDER_SCHEDULED_PREFIX, substring = true).assertIsDisplayed()
        }

    private companion object {
        const val BACKUP_ROW_LABEL = "Backup & restore"
        const val BACKUP_EXPORT_ACTION = "Export backup"
        const val RELEASE_REMINDERS_LABEL = "Release reminders"
        const val REMINDER_TIME_LABEL = "Reminder time"

        // Desktop's is24HourClock() is always false, so the 12-hour form is expected.
        const val DEFAULT_REMINDER_TIME = "9:00 AM"
        const val DEBUG_FIRE_TEST_REMINDER = "Fire test reminder in 1 minute"
        const val DEBUG_POLL_RELEASE_REMINDERS = "Poll release reminders now"
        const val DEBUG_SHOW_PENDING = "Show pending reminders"
        const val DEBUG_CLEAR_REMINDERS = "Clear all reminders"
        const val DEBUG_POLL_EPISODES = "Poll episode notifications now"
        const val PENDING_REMINDERS_EMPTY = "Nothing scheduled"
        const val CLOSE_ACTION = "Close"
        const val TEST_REMINDER_SCHEDULED_PREFIX = "Test reminder scheduled for"
    }
}
