package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import com.ajinkyabadve.kmmmywatchlist.core.notification.FakeNotificationPermissionRequester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccountScreenModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateIsReadFromTheRepositories() =
        runTest {
            val fixture = AccountTestFixture(restrictedModeEnabled = true, episodeNotificationsEnabled = true)

            val state = fixture.screenModel.uiState.value

            assertEquals(AccountTestConstant.INITIAL_REGION, state.regionCode)
            assertEquals(AccountTestConstant.INITIAL_FALLBACK_REGION, state.fallbackRegionCode)
            assertTrue(state.restrictedModeEnabled)
            assertTrue(state.episodeNotificationsEnabled)
        }

    @Test
    fun testSelectingARegionSavesItAndReResolvesReminderDates() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.screenModel.onRegionSelected(AccountTestConstant.NEW_REGION)

            assertEquals(listOf(AccountTestConstant.NEW_REGION), fixture.regions.setRegionCalls)
            assertEquals(AccountTestConstant.NEW_REGION, fixture.screenModel.uiState.value.regionCode)
            assertEquals(1, fixture.releaseReminderPolls)
        }

    @Test
    fun testSelectingAFallbackRegionSavesItAndReResolvesReminderDates() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.screenModel.onFallbackRegionSelected(AccountTestConstant.NEW_FALLBACK_REGION)

            assertEquals(listOf(AccountTestConstant.NEW_FALLBACK_REGION), fixture.regions.setFallbackRegionCalls)
            assertEquals(AccountTestConstant.NEW_FALLBACK_REGION, fixture.screenModel.uiState.value.fallbackRegionCode)
            assertEquals(1, fixture.releaseReminderPolls)
        }

    @Test
    fun testRegionChangeSkipsTheReminderPollWhereRemindersAreUnsupported() =
        runTest {
            val fixture = AccountTestFixture(releaseRemindersSupported = false)

            fixture.screenModel.onRegionSelected(AccountTestConstant.NEW_REGION)

            assertEquals(0, fixture.releaseReminderPolls)
        }

    @Test
    fun testRestrictedModeChangeIsPersisted() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.screenModel.onRestrictedModeChanged(true)

            assertEquals(listOf(true), fixture.restrictedMode.setRestrictedModeCalls)
            assertTrue(fixture.screenModel.uiState.value.restrictedModeEnabled)
        }

    @Test
    fun testEnablingEpisodeNotificationsPersistsAndStartsTheBackgroundJob() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.screenModel.onEpisodeNotificationsChanged(true, FakeNotificationPermissionRequester(granted = true))

            assertEquals(listOf(true), fixture.notificationSettings.setEpisodeNotificationsEnabledCalls)
            assertTrue(fixture.screenModel.uiState.value.episodeNotificationsEnabled)
            assertEquals(1, fixture.scheduleJobCalls)
        }

    @Test
    fun testDisablingEpisodeNotificationsWithNoRemindersStopsTheBackgroundJob() =
        runTest {
            val fixture = AccountTestFixture(episodeNotificationsEnabled = true)

            fixture.screenModel.onEpisodeNotificationsChanged(false, FakeNotificationPermissionRequester())

            assertFalse(fixture.screenModel.uiState.value.episodeNotificationsEnabled)
            assertEquals(1, fixture.cancelJobCalls)
        }

    @Test
    fun testReminderPreferenceIsObservedFromTheRepository() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.reminders.setPreference(fixture.reminders.preference().copy(time = LATER_TIME))

            assertEquals(LATER_TIME, fixture.screenModel.uiState.value.reminderPreference.time)
        }

    @Test
    fun testTurningReleaseRemindersOffSavesThePreferenceAndRefreshesTheJob() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.screenModel.onReleaseRemindersChanged(false, FakeNotificationPermissionRequester())

            assertFalse(
                fixture.reminders.setPreferenceCalls
                    .single()
                    .enabled,
            )
            assertFalse(fixture.screenModel.uiState.value.reminderPreference.enabled)
            assertEquals(1, fixture.cancelJobCalls)
        }

    @Test
    fun testChangingTheReminderTimeKeepsTheEnabledFlag() =
        runTest {
            val fixture = AccountTestFixture()
            val enabledBefore = fixture.screenModel.uiState.value.reminderPreference.enabled

            fixture.screenModel.onReminderTimeChanged(LATER_TIME)

            val saved = fixture.reminders.setPreferenceCalls.single()
            assertEquals(LATER_TIME, saved.time)
            assertEquals(enabledBefore, saved.enabled)
        }

    @Test
    fun testReloadPicksUpSettingsChangedBehindTheScreen() =
        runTest {
            val fixture = AccountTestFixture()
            // A backup restore writes straight to the repositories.
            fixture.regions.setSelectedRegion(AccountTestConstant.NEW_REGION)
            fixture.restrictedMode.setRestrictedModeEnabled(true)

            fixture.screenModel.reload()

            assertEquals(AccountTestConstant.NEW_REGION, fixture.screenModel.uiState.value.regionCode)
            assertTrue(fixture.screenModel.uiState.value.restrictedModeEnabled)
        }

    @Test
    fun testDebugPollRowsRunTheirOwnPollOnly() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.screenModel.onDebugPollEpisodeNotifications()
            fixture.screenModel.onDebugPollPersonNotifications()
            fixture.screenModel.onDebugPollPersonNotifications()
            fixture.screenModel.onDebugPollCollectionNotifications()
            fixture.screenModel.onDebugPollReleaseReminders()

            assertEquals(1, fixture.debugActions.episodePollCount)
            assertEquals(2, fixture.debugActions.personPollCount)
            assertEquals(1, fixture.debugActions.collectionPollCount)
            assertEquals(1, fixture.releaseReminderPolls)
        }

    @Test
    fun testDebugResetEpisodeAlertPromptTurnsAlertsOff() =
        runTest {
            val fixture = AccountTestFixture(episodeNotificationsEnabled = true)

            fixture.screenModel.onDebugResetEpisodeAlertPrompt()

            assertEquals(listOf(false), fixture.notificationSettings.setEpisodeNotificationsEnabledCalls)
            assertFalse(fixture.notificationSettings.hasSeenEpisodeAlertOptInPrompt())
            assertFalse(fixture.screenModel.uiState.value.episodeNotificationsEnabled)
            assertEquals(1, fixture.cancelJobCalls)
        }

    @Test
    fun testDebugFireTestReminderSchedulesItAndShowsItsTimeUntilDismissed() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.screenModel.onDebugFireTestReminder(FakeNotificationPermissionRequester(granted = true))

            assertEquals(1, fixture.scheduler.scheduled.size)
            assertTrue(
                fixture.screenModel.uiState.value.testReminderMessage
                    .orEmpty()
                    .startsWith(TEST_REMINDER_PREFIX),
            )

            fixture.screenModel.dismissTestReminderNotice()

            assertNull(fixture.screenModel.uiState.value.testReminderMessage)
        }

    @Test
    fun testDebugShowPendingRemindersFillsTheListUntilDismissed() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.screenModel.onDebugShowPendingReminders()

            assertNotNull(fixture.screenModel.uiState.value.pendingReminders)

            fixture.screenModel.dismissPendingReminders()

            assertNull(fixture.screenModel.uiState.value.pendingReminders)
        }

    @Test
    fun testDebugClearRemindersDeletesThemAndRefreshesTheJob() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.screenModel.onDebugClearReminders()

            assertEquals(1, fixture.reminders.deleteAllCallCount)
            assertEquals(1, fixture.cancelJobCalls)
        }

    @Test
    fun testEpisodeNotificationsStayOffWhenPermissionIsDenied() =
        runTest {
            val fixture = AccountTestFixture()
            val requester = FakeNotificationPermissionRequester(granted = false)

            fixture.screenModel.onEpisodeNotificationsChanged(true, requester)

            assertEquals(1, requester.requestCount)
            assertTrue(fixture.notificationSettings.setEpisodeNotificationsEnabledCalls.isEmpty())
            assertFalse(fixture.screenModel.uiState.value.episodeNotificationsEnabled)
            assertEquals(0, fixture.scheduleJobCalls)
        }

    @Test
    fun testTurningANotificationSwitchOffNeverAsksForPermission() =
        runTest {
            val fixture = AccountTestFixture(episodeNotificationsEnabled = true)
            val requester = FakeNotificationPermissionRequester(granted = false)

            fixture.screenModel.onEpisodeNotificationsChanged(false, requester)
            fixture.screenModel.onReleaseRemindersChanged(false, requester)

            assertEquals(0, requester.requestCount)
            assertFalse(fixture.screenModel.uiState.value.episodeNotificationsEnabled)
        }

    @Test
    fun testReleaseRemindersStayOffWhenPermissionIsDenied() =
        runTest {
            val fixture = AccountTestFixture()
            fixture.reminders.setPreference(fixture.reminders.preference().copy(enabled = false))
            fixture.reminders.setPreferenceCalls.clear()

            fixture.screenModel.onReleaseRemindersChanged(true, FakeNotificationPermissionRequester(granted = false))

            assertTrue(fixture.reminders.setPreferenceCalls.isEmpty())
            assertFalse(fixture.screenModel.uiState.value.reminderPreference.enabled)
        }

    @Test
    fun testDebugFireTestReminderSchedulesNothingWhenPermissionIsDenied() =
        runTest {
            val fixture = AccountTestFixture()

            fixture.screenModel.onDebugFireTestReminder(FakeNotificationPermissionRequester(granted = false))

            assertTrue(fixture.scheduler.scheduled.isEmpty())
            assertNull(fixture.screenModel.uiState.value.testReminderMessage)
        }

    @Test
    fun testEachPickerRowOpensItsDialogAndDismissClosesIt() =
        runTest {
            val fixture = AccountTestFixture()
            val screenModel = fixture.screenModel

            screenModel.onRegionRowClicked()
            assertEquals(AccountDialog.REGION, screenModel.uiState.value.openDialog)
            screenModel.onFallbackRegionRowClicked()
            assertEquals(AccountDialog.FALLBACK_REGION, screenModel.uiState.value.openDialog)
            screenModel.onReminderTimeRowClicked()
            assertEquals(AccountDialog.REMINDER_TIME, screenModel.uiState.value.openDialog)

            screenModel.dismissDialog()

            assertNull(screenModel.uiState.value.openDialog)
        }

    @Test
    fun testConfirmingAReminderTimeClosesThePicker() =
        runTest {
            val fixture = AccountTestFixture()
            fixture.screenModel.onReminderTimeRowClicked()

            fixture.screenModel.onReminderTimeChanged(LATER_TIME)

            assertNull(fixture.screenModel.uiState.value.openDialog)
        }

    @Test
    fun testReminderTimeIsFormattedForTheRow() =
        runTest {
            val fixture = AccountTestFixture()

            assertEquals(DEFAULT_TIME_TEXT, fixture.screenModel.uiState.value.reminderTimeText)

            fixture.reminders.setPreference(fixture.reminders.preference().copy(time = LATER_TIME))

            assertEquals(LATER_TIME_TEXT, fixture.screenModel.uiState.value.reminderTimeText)
        }

    @Test
    fun testRowVisibilityFollowsThePlatform() =
        runTest {
            val fixture = AccountTestFixture(releaseRemindersSupported = false, backupSupported = false, debugBuild = false)

            val visibility = fixture.screenModel.uiState.value.visibility

            assertFalse(visibility.releaseReminders)
            assertFalse(visibility.backup)
            assertFalse(visibility.debug)
        }

    private companion object {
        val LATER_TIME = LocalTime(18, 30)

        // The fixture formats on a 12-hour clock.
        const val DEFAULT_TIME_TEXT = "9:00 AM"
        const val LATER_TIME_TEXT = "6:30 PM"
        const val TEST_REMINDER_PREFIX = "Test reminder scheduled for"
    }
}
