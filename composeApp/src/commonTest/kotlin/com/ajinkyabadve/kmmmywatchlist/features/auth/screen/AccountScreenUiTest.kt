package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

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
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.backup.screen.BackupTestFixture
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeNotificationSettingsRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRestrictedModeRepository
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
            val fakeRestrictedModeRepository = FakeRestrictedModeRepository(restrictedModeEnabled = true)

            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                    restrictedModeRepository = fakeRestrictedModeRepository,
                )
            }

            onAllNodes(isToggleable())[0].performClick()

            assertTrue(fakeRestrictedModeRepository.setRestrictedModeCalls.contains(false))
        }

    @Test
    fun testTogglingEpisodeNotificationsOnRequestsPermissionAndPersists() =
        runComposeUiTest {
            val session =
                UserSession(sessionId = "session_999", accountId = 99L, username = "jane_doe", name = "Jane Doe")
            fakeAuthRepository.saveSession(session)
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            val fakeNotificationSettingsRepository = FakeNotificationSettingsRepository(episodeNotificationsEnabled = false)

            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    screenModel = screenModel,
                    notificationSettingsRepository = fakeNotificationSettingsRepository,
                )
            }

            onAllNodes(isToggleable())[1].performClick()
            waitForIdle()

            assertTrue(fakeNotificationSettingsRepository.setEpisodeNotificationsEnabledCalls.contains(true))
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
                    notificationSettingsRepository = FakeNotificationSettingsRepository(),
                    releaseReminderRepository = FakeReleaseReminderRepository(),
                    showReleaseReminderSettings = true,
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
                    notificationSettingsRepository = FakeNotificationSettingsRepository(),
                    releaseReminderRepository = FakeReleaseReminderRepository(),
                    showReleaseReminderSettings = true,
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
            val releaseReminderRepository = FakeReleaseReminderRepository()
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    notificationSettingsRepository = FakeNotificationSettingsRepository(),
                    releaseReminderRepository = releaseReminderRepository,
                    showReleaseReminderSettings = true,
                    screenModel = screenModel,
                )
            }

            onNodeWithText(DEBUG_CLEAR_REMINDERS).performScrollTo().performClick()
            waitUntil { releaseReminderRepository.deleteAllCallCount == 1 }

            assertEquals(1, releaseReminderRepository.deleteAllCallCount)
        }

    @Test
    fun testReleaseReminderRowsAreHiddenWhereRemindersCantFire() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    releaseReminderRepository = FakeReleaseReminderRepository(),
                    showReleaseReminderSettings = false,
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
                    showBackupSettings = true,
                    backupFileLauncher = FakeBackupFileLauncher(),
                    backupScreenModel = BackupTestFixture().screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithText(BACKUP_ROW_LABEL).performScrollTo().performClick()

            onNodeWithText(BACKUP_EXPORT_ACTION).assertIsDisplayed()
        }

    @Test
    fun testBackupRowIsHiddenWhereBackupIsUnsupported() =
        runComposeUiTest {
            val screenModel = AuthScreenModel(authRepository = fakeAuthRepository)
            setContent {
                AccountScreen(
                    isDialogPresentation = false,
                    onBackClicked = {},
                    webAuthLauncher = fakeWebAuthLauncher,
                    showBackupSettings = false,
                    backupFileLauncher = FakeBackupFileLauncher(),
                    backupScreenModel = BackupTestFixture().screenModel,
                    screenModel = screenModel,
                )
            }

            onNodeWithText(BACKUP_ROW_LABEL).assertDoesNotExist()
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
    }
}
