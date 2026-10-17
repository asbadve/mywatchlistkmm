package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.formatClockTime
import com.ajinkyabadve.kmmmywatchlist.core.notification.NotificationPermissionRequester
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationDebugActions
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationDebugActionsImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationJobSync
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderCoordinator
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.NotificationSettingsRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.NotificationSettingsRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RestrictedModeRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RestrictedModeRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.is24HourClock
import com.ajinkyabadve.kmmmywatchlist.isDebugBuild
import com.ajinkyabadve.kmmmywatchlist.isMobilePlatform
import com.ajinkyabadve.kmmmywatchlist.supportsLocalBackup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.debug_fire_test_reminder_scheduled
import mywatchlist.composeapp.generated.resources.time_am
import mywatchlist.composeapp.generated.resources.time_pm
import org.jetbrains.compose.resources.getString

/**
 * Owns the settings half of [AccountScreen]: region, restricted mode, notification switches,
 * reminder time, and the debug rows. Sign-in state stays in the shared [AuthScreenModel] (the My
 * Fav tab uses it too), so the account screen holds both.
 *
 * Turning a notification switch on asks for the OS permission first. The requester itself has to be
 * created by the screen (it registers a launcher with the composition on Android), so those
 * methods take it as an argument - but whether to ask, and what to do with the answer, is decided
 * here.
 *
 * Texts that include the time (the reminder-time row, the test-reminder confirmation) are resolved
 * here too, through compose-resources' non-composable `getString`.
 *
 * @param releaseRemindersSupported reminders can only be delivered on Android/iOS; elsewhere their
 *   rows are hidden, the preference isn't watched and a region change doesn't re-resolve dates.
 * @param backupSupported hides the backup row on the web target (item 15).
 * @param debugBuild shows the debug rows.
 * @param pollReleaseReminders re-resolves every saved reminder's date for the current region and
 *   reschedules it - a function rather than a [ReleaseReminderPoller] so tests skip its network calls.
 */
class AccountScreenModel(
    private val regionRepository: RegionRepository = RegionRepositoryImpl(),
    private val restrictedModeRepository: RestrictedModeRepository = RestrictedModeRepositoryImpl(),
    private val notificationSettingsRepository: NotificationSettingsRepository = NotificationSettingsRepositoryImpl(),
    releaseReminderRepository: ReleaseReminderRepository = ReleaseReminderRepositoryImpl(),
    private val releaseReminderCoordinator: ReleaseReminderCoordinator = ReleaseReminderCoordinator(releaseReminderRepository),
    private val notificationJobSync: NotificationJobSync =
        NotificationJobSync(notificationSettingsRepository, releaseReminderRepository),
    private val pollReleaseReminders: suspend () -> Unit = {
        ReleaseReminderPoller(
            repository = releaseReminderRepository,
            regionRepository = regionRepository,
            coordinator = releaseReminderCoordinator,
        ).poll()
    },
    private val notificationDebugActions: NotificationDebugActions = NotificationDebugActionsImpl(),
    private val releaseRemindersSupported: Boolean = isMobilePlatform(),
    backupSupported: Boolean = supportsLocalBackup(),
    debugBuild: Boolean = isDebugBuild(),
    private val is24Hour: Boolean = is24HourClock(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val visibility =
        SettingsRowVisibility(releaseReminders = releaseRemindersSupported, backup = backupSupported, debug = debugBuild)
    private val _uiState = MutableStateFlow(readSettings())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        if (releaseRemindersSupported) {
            viewModelScope.launch {
                releaseReminderRepository.observePreference().collect { preference ->
                    val timeText = formatTime(preference.time)
                    _uiState.update { it.copy(reminderPreference = preference, reminderTimeText = timeText) }
                }
            }
        }
    }

    fun onRegionRowClicked() = openDialog(AccountDialog.REGION)

    fun onFallbackRegionRowClicked() = openDialog(AccountDialog.FALLBACK_REGION)

    fun onReminderTimeRowClicked() = openDialog(AccountDialog.REMINDER_TIME)

    fun dismissDialog() {
        _uiState.update { it.copy(openDialog = null) }
    }

    fun onRegionSelected(regionCode: String) {
        regionRepository.setSelectedRegion(regionCode)
        _uiState.update { it.copy(regionCode = regionCode) }
        refreshReminderDatesForRegion()
    }

    fun onFallbackRegionSelected(regionCode: String) {
        regionRepository.setFallbackRegion(regionCode)
        _uiState.update { it.copy(fallbackRegionCode = regionCode) }
        refreshReminderDatesForRegion()
    }

    fun onRestrictedModeChanged(enabled: Boolean) {
        restrictedModeRepository.setRestrictedModeEnabled(enabled)
        _uiState.update { it.copy(restrictedModeEnabled = enabled) }
    }

    /** Turning on asks [permissionRequester] first and stays off if permission is denied. */
    fun onEpisodeNotificationsChanged(
        enabled: Boolean,
        permissionRequester: NotificationPermissionRequester,
    ) {
        viewModelScope.launch {
            if (enabled && !permissionRequester.request()) return@launch
            notificationSettingsRepository.setEpisodeNotificationsEnabled(enabled)
            _uiState.update { it.copy(episodeNotificationsEnabled = enabled) }
            // Turning episode alerts off keeps the shared background job running if release
            // reminders still need it - see NotificationJobSync's kdoc.
            notificationJobSync.refresh()
        }
    }

    /** Turning on asks [permissionRequester] first and stays off if permission is denied. */
    fun onReleaseRemindersChanged(
        enabled: Boolean,
        permissionRequester: NotificationPermissionRequester,
    ) {
        viewModelScope.launch {
            if (enabled && !permissionRequester.request()) return@launch
            releaseReminderCoordinator.setPreference(_uiState.value.reminderPreference.copy(enabled = enabled))
            notificationJobSync.refresh()
        }
    }

    fun onReminderTimeChanged(time: LocalTime) {
        dismissDialog()
        viewModelScope.launch {
            releaseReminderCoordinator.setPreference(_uiState.value.reminderPreference.copy(time = time))
        }
    }

    /** Re-reads every setting after a backup restore, which may have changed any of them. */
    fun reload() {
        val restored = readSettings()
        _uiState.update {
            it.copy(
                regionCode = restored.regionCode,
                fallbackRegionCode = restored.fallbackRegionCode,
                restrictedModeEnabled = restored.restrictedModeEnabled,
                episodeNotificationsEnabled = restored.episodeNotificationsEnabled,
            )
        }
    }

    fun onDebugPollEpisodeNotifications() {
        viewModelScope.launch { notificationDebugActions.pollEpisodeNotificationsNow() }
    }

    fun onDebugPollPersonNotifications() {
        viewModelScope.launch { notificationDebugActions.pollPersonNotificationsNow() }
    }

    fun onDebugPollCollectionNotifications() {
        viewModelScope.launch { notificationDebugActions.pollCollectionNotificationsNow() }
    }

    /** Turns episode alerts off and makes the one-time opt-in prompt show again. */
    fun onDebugResetEpisodeAlertPrompt() {
        notificationSettingsRepository.setEpisodeNotificationsEnabled(false)
        notificationSettingsRepository.resetEpisodeAlertOptInPromptForDebug()
        _uiState.update { it.copy(episodeNotificationsEnabled = false) }
        viewModelScope.launch { notificationJobSync.refresh() }
    }

    /** Schedules nothing if [permissionRequester] is denied - the reminder could never show. */
    fun onDebugFireTestReminder(permissionRequester: NotificationPermissionRequester) {
        viewModelScope.launch {
            if (!permissionRequester.request()) return@launch
            val fireAt = releaseReminderCoordinator.scheduleTestReminder()
            val message = getString(Res.string.debug_fire_test_reminder_scheduled, formatTime(fireAt.time))
            _uiState.update { it.copy(testReminderMessage = message) }
        }
    }

    fun onDebugPollReleaseReminders() {
        viewModelScope.launch { pollReleaseReminders() }
    }

    fun onDebugShowPendingReminders() {
        viewModelScope.launch {
            val pending = releaseReminderCoordinator.pendingForDebug()
            _uiState.update { it.copy(pendingReminders = pending) }
        }
    }

    fun onDebugClearReminders() {
        viewModelScope.launch {
            releaseReminderCoordinator.clearAll()
            notificationJobSync.refresh()
        }
    }

    fun dismissPendingReminders() {
        _uiState.update { it.copy(pendingReminders = null) }
    }

    fun dismissTestReminderNotice() {
        _uiState.update { it.copy(testReminderMessage = null) }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }

    /**
     * A reminder's date is resolved for the viewer's region (a movie can open weeks apart in two
     * countries), so a region change re-resolves every saved reminder now rather than leaving it
     * on the old region's date until the next background poll.
     */
    private fun refreshReminderDatesForRegion() {
        if (releaseRemindersSupported) {
            viewModelScope.launch { pollReleaseReminders() }
        }
    }

    private fun openDialog(dialog: AccountDialog) {
        _uiState.update { it.copy(openDialog = dialog) }
    }

    private suspend fun formatTime(time: LocalTime): String =
        formatClockTime(time, is24Hour, getString(Res.string.time_am), getString(Res.string.time_pm))

    private fun readSettings(): AccountUiState =
        AccountUiState(
            regionCode = regionRepository.getSelectedRegion(),
            fallbackRegionCode = regionRepository.getFallbackRegion(),
            restrictedModeEnabled = restrictedModeRepository.isRestrictedModeEnabled(),
            episodeNotificationsEnabled = notificationSettingsRepository.isEpisodeNotificationsEnabled(),
            visibility = visibility,
            is24Hour = is24Hour,
        )
}
