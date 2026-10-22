package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import com.ajinkyabadve.kmmmywatchlist.core.notification.PendingReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.model.ReminderPreference
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderConstant

/**
 * Everything the account screen's settings rows and dialogs show.
 *
 * @param reminderTimeText [reminderPreference]'s time, already formatted for the device's 12/24-hour
 *   setting ([is24Hour], which the time picker also needs).
 * @param openDialog the picker currently open, or null.
 * @param pendingReminders/[testReminderMessage] results of debug actions - non-null while their
 *   dialog is open.
 */
data class AccountUiState(
    val regionCode: String,
    val fallbackRegionCode: String,
    val restrictedModeEnabled: Boolean,
    val episodeNotificationsEnabled: Boolean,
    val visibility: SettingsRowVisibility,
    val is24Hour: Boolean,
    val reminderPreference: ReminderPreference =
        ReminderPreference(enabled = ReleaseReminderConstant.DEFAULT_ENABLED, time = ReleaseReminderConstant.DEFAULT_TIME),
    val reminderTimeText: String = "",
    val openDialog: AccountDialog? = null,
    val pendingReminders: List<PendingReminder>? = null,
    val testReminderMessage: String? = null,
)
