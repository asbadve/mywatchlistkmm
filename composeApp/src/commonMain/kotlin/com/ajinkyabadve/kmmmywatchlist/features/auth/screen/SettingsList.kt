package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.core.format.toRegionFlagEmoji
import com.ajinkyabadve.kmmmywatchlist.design.settings.SettingsRow
import com.ajinkyabadve.kmmmywatchlist.design.settings.SettingsSwitchRow
import com.ajinkyabadve.kmmmywatchlist.features.auth.viewmodel.AccountScreenModel
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.auth_logout
import mywatchlist.composeapp.generated.resources.debug_clear_reminders_description
import mywatchlist.composeapp.generated.resources.debug_clear_reminders_label
import mywatchlist.composeapp.generated.resources.debug_fire_test_reminder_description
import mywatchlist.composeapp.generated.resources.debug_fire_test_reminder_label
import mywatchlist.composeapp.generated.resources.debug_poll_collection_notifications_now_description
import mywatchlist.composeapp.generated.resources.debug_poll_collection_notifications_now_label
import mywatchlist.composeapp.generated.resources.debug_poll_notifications_now_description
import mywatchlist.composeapp.generated.resources.debug_poll_notifications_now_label
import mywatchlist.composeapp.generated.resources.debug_poll_person_notifications_now_description
import mywatchlist.composeapp.generated.resources.debug_poll_person_notifications_now_label
import mywatchlist.composeapp.generated.resources.debug_poll_release_reminders_description
import mywatchlist.composeapp.generated.resources.debug_poll_release_reminders_label
import mywatchlist.composeapp.generated.resources.debug_reset_episode_alert_prompt_description
import mywatchlist.composeapp.generated.resources.debug_reset_episode_alert_prompt_label
import mywatchlist.composeapp.generated.resources.debug_show_pending_reminders_description
import mywatchlist.composeapp.generated.resources.debug_show_pending_reminders_label
import mywatchlist.composeapp.generated.resources.settings_backup_description
import mywatchlist.composeapp.generated.resources.settings_backup_label
import mywatchlist.composeapp.generated.resources.settings_episode_notifications_description
import mywatchlist.composeapp.generated.resources.settings_episode_notifications_title
import mywatchlist.composeapp.generated.resources.settings_fallback_region_description
import mywatchlist.composeapp.generated.resources.settings_fallback_region_label
import mywatchlist.composeapp.generated.resources.settings_privacy_policy_label
import mywatchlist.composeapp.generated.resources.settings_region_label
import mywatchlist.composeapp.generated.resources.settings_release_reminders_description
import mywatchlist.composeapp.generated.resources.settings_release_reminders_title
import mywatchlist.composeapp.generated.resources.settings_reminder_time_description
import mywatchlist.composeapp.generated.resources.settings_reminder_time_label
import mywatchlist.composeapp.generated.resources.settings_restricted_mode_description
import mywatchlist.composeapp.generated.resources.settings_restricted_mode_label
import mywatchlist.composeapp.generated.resources.settings_trending_auto_scroll_description
import mywatchlist.composeapp.generated.resources.settings_trending_auto_scroll_label
import mywatchlist.composeapp.generated.resources.tmdb_attribution_notice
import org.jetbrains.compose.resources.stringResource

/**
 * The account screen's settings rows, top to bottom: region, fallback region, restricted mode,
 * trending autoscroll, episode notifications, release reminders + reminder time, backup, the debug
 * rows, log out, privacy policy and the TMDB attribution notice.
 *
 * Presentational only (code-conventions §8): plain values in, one callback per row out. What a
 * click does - including asking for the notification permission - is [AccountScreenModel]'s call.
 *
 * @param onLogoutClicked null hides the row (there's no session to end while signed out).
 */
@Composable
internal fun SettingsList(
    uiState: AccountUiState,
    onRegionClicked: () -> Unit,
    onFallbackRegionClicked: () -> Unit,
    onRestrictedModeChanged: (Boolean) -> Unit,
    onTrendingAutoScrollChanged: (Boolean) -> Unit,
    onEpisodeNotificationsChanged: (Boolean) -> Unit,
    onReleaseRemindersChanged: (Boolean) -> Unit,
    onReminderTimeClicked: () -> Unit,
    onBackupClicked: () -> Unit,
    debugCallbacks: SettingsDebugCallbacks,
    onLogoutClicked: (() -> Unit)?,
    onPrivacyPolicyClicked: () -> Unit,
) {
    val visibility = uiState.visibility
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsRow(
            label = stringResource(Res.string.settings_region_label),
            value = "${uiState.regionCode.toRegionFlagEmoji()} ${uiState.regionCode}".trim(),
            onClick = onRegionClicked,
        )
        SettingsRow(
            label = stringResource(Res.string.settings_fallback_region_label),
            value = "${uiState.fallbackRegionCode.toRegionFlagEmoji()} ${uiState.fallbackRegionCode}".trim(),
            onClick = onFallbackRegionClicked,
            description = stringResource(Res.string.settings_fallback_region_description),
        )
        SettingsSwitchRow(
            label = stringResource(Res.string.settings_restricted_mode_label),
            description = stringResource(Res.string.settings_restricted_mode_description),
            checked = uiState.restrictedModeEnabled,
            onCheckedChange = onRestrictedModeChanged,
        )
        SettingsSwitchRow(
            label = stringResource(Res.string.settings_trending_auto_scroll_label),
            description = stringResource(Res.string.settings_trending_auto_scroll_description),
            checked = uiState.trendingAutoScrollEnabled,
            onCheckedChange = onTrendingAutoScrollChanged,
        )
        SettingsSwitchRow(
            label = stringResource(Res.string.settings_episode_notifications_title),
            description = stringResource(Res.string.settings_episode_notifications_description),
            checked = uiState.episodeNotificationsEnabled,
            onCheckedChange = onEpisodeNotificationsChanged,
        )
        if (visibility.releaseReminders) {
            SettingsSwitchRow(
                label = stringResource(Res.string.settings_release_reminders_title),
                description = stringResource(Res.string.settings_release_reminders_description),
                checked = uiState.reminderPreference.enabled,
                onCheckedChange = onReleaseRemindersChanged,
            )
            SettingsRow(
                label = stringResource(Res.string.settings_reminder_time_label),
                description = stringResource(Res.string.settings_reminder_time_description),
                value = uiState.reminderTimeText,
                onClick = onReminderTimeClicked,
            )
        }
        if (visibility.backup) {
            SettingsRow(
                label = stringResource(Res.string.settings_backup_label),
                description = stringResource(Res.string.settings_backup_description),
                onClick = onBackupClicked,
            )
        }
        if (visibility.debug) {
            DebugRows(showReleaseReminderRows = visibility.releaseReminders, callbacks = debugCallbacks)
        }
        onLogoutClicked?.let {
            SettingsRow(
                label = stringResource(Res.string.auth_logout),
                onClick = it,
                labelColor = MaterialTheme.colorScheme.error,
            )
        }
        SettingsRow(
            label = stringResource(Res.string.settings_privacy_policy_label),
            onClick = onPrivacyPolicyClicked,
        )
        Text(
            text = stringResource(Res.string.tmdb_attribution_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun DebugRows(
    showReleaseReminderRows: Boolean,
    callbacks: SettingsDebugCallbacks,
) {
    SettingsRow(
        label = stringResource(Res.string.debug_poll_notifications_now_label),
        description = stringResource(Res.string.debug_poll_notifications_now_description),
        onClick = callbacks.onPollEpisodeNotificationsClicked,
    )
    SettingsRow(
        label = stringResource(Res.string.debug_poll_person_notifications_now_label),
        description = stringResource(Res.string.debug_poll_person_notifications_now_description),
        onClick = callbacks.onPollPersonNotificationsClicked,
    )
    SettingsRow(
        label = stringResource(Res.string.debug_poll_collection_notifications_now_label),
        description = stringResource(Res.string.debug_poll_collection_notifications_now_description),
        onClick = callbacks.onPollCollectionNotificationsClicked,
    )
    SettingsRow(
        label = stringResource(Res.string.debug_reset_episode_alert_prompt_label),
        description = stringResource(Res.string.debug_reset_episode_alert_prompt_description),
        onClick = callbacks.onResetEpisodeAlertPromptClicked,
    )
    if (showReleaseReminderRows) {
        SettingsRow(
            label = stringResource(Res.string.debug_fire_test_reminder_label),
            description = stringResource(Res.string.debug_fire_test_reminder_description),
            onClick = callbacks.onFireTestReminderClicked,
        )
        SettingsRow(
            label = stringResource(Res.string.debug_poll_release_reminders_label),
            description = stringResource(Res.string.debug_poll_release_reminders_description),
            onClick = callbacks.onPollReleaseRemindersClicked,
        )
        SettingsRow(
            label = stringResource(Res.string.debug_show_pending_reminders_label),
            description = stringResource(Res.string.debug_show_pending_reminders_description),
            onClick = callbacks.onShowPendingRemindersClicked,
        )
        SettingsRow(
            label = stringResource(Res.string.debug_clear_reminders_label),
            description = stringResource(Res.string.debug_clear_reminders_description),
            onClick = callbacks.onClearRemindersClicked,
        )
    }
}
