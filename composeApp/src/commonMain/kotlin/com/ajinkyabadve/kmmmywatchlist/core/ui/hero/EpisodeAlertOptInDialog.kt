package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.episode_alert_prompt_body
import mywatchlist.composeapp.generated.resources.episode_alert_prompt_confirm
import mywatchlist.composeapp.generated.resources.episode_alert_prompt_dismiss
import mywatchlist.composeapp.generated.resources.episode_alert_prompt_fineprint
import mywatchlist.composeapp.generated.resources.episode_alert_prompt_title
import org.jetbrains.compose.resources.stringResource

private object EpisodeAlertOptInDialogConstant {
    val ICON_BADGE_SIZE = 44.dp
    val ICON_BADGE_CORNER_RADIUS = 13.dp
    val ICON_SIZE = 22.dp
}

/**
 * The in-context "turn on episode alerts?" prompt shown the first time a TV show is favorited or
 * watchlisted while notifications are off - see `future_features_checklist.md` item 3a's follow-up
 * and the design artifact it was built from ("Episode Alerts Prompt"). [MediaActionButtonsSection]
 * decides *when* to show this (its [MediaActionsState.shouldPromptForEpisodeAlerts] gate, TV-only,
 * once-ever via [NotificationSettingsRepository.hasSeenEpisodeAlertOptInPrompt]) - this composable
 * only renders the ask and reports which button was tapped, per code-conventions §7.
 *
 * Uses [AlertDialog], the dialog primitive already established by `AddToListDialog` elsewhere in
 * this codebase, rather than introducing `ModalBottomSheet` as a new one just for this prompt.
 */
@Composable
internal fun EpisodeAlertOptInDialog(
    tvShowName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier =
                    Modifier
                        .size(EpisodeAlertOptInDialogConstant.ICON_BADGE_SIZE)
                        .clip(RoundedCornerShape(EpisodeAlertOptInDialogConstant.ICON_BADGE_CORNER_RADIUS)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(EpisodeAlertOptInDialogConstant.ICON_SIZE),
                )
            }
        },
        title = { Text(stringResource(Res.string.episode_alert_prompt_title, tvShowName)) },
        text = {
            Column {
                Text(stringResource(Res.string.episode_alert_prompt_body))
                Text(
                    text = stringResource(Res.string.episode_alert_prompt_fineprint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.episode_alert_prompt_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.episode_alert_prompt_dismiss))
            }
        },
    )
}
