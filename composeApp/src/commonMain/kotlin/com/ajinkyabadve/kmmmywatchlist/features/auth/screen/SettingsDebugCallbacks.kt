package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

/** The debug-build rows' click handlers, grouped so [SettingsList] keeps a readable signature. */
data class SettingsDebugCallbacks(
    val onPollEpisodeNotificationsClicked: () -> Unit,
    val onPollPersonNotificationsClicked: () -> Unit,
    val onPollCollectionNotificationsClicked: () -> Unit,
    val onResetEpisodeAlertPromptClicked: () -> Unit,
    val onFireTestReminderClicked: () -> Unit,
    val onPollReleaseRemindersClicked: () -> Unit,
    val onShowPendingRemindersClicked: () -> Unit,
    val onClearRemindersClicked: () -> Unit,
)
