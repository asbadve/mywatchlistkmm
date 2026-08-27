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
import mywatchlist.composeapp.generated.resources.notification_opt_in_confirm
import mywatchlist.composeapp.generated.resources.notification_opt_in_dismiss
import mywatchlist.composeapp.generated.resources.notification_opt_in_fineprint
import org.jetbrains.compose.resources.stringResource

private object NotificationOptInDialogConstant {
    val ICON_BADGE_SIZE = 44.dp
    val ICON_BADGE_CORNER_RADIUS = 13.dp
    val ICON_SIZE = 22.dp
}

/**
 * The in-context "turn on notifications?" prompt - shared shell for both notification kinds this
 * app asks about: the first time a TV show is favorited/watchlisted while notifications are off
 * (see `MediaActionButtonsSection`, [title]/[body] from `episode_alert_prompt_*`), and the first
 * time a person is followed while notifications are off (see `PersonDetailScreen`, [title]/[body]
 * from `person_alert_prompt_*`) - see `future_features_checklist.md` item 3a/3b and the design
 * artifact this was originally built from ("Episode Alerts Prompt"). The caller decides *when* to
 * show this and *what* [onConfirm]/[onDismiss] do (both callers request the OS permission then
 * flip the same shared `NotificationSettingsRepository` setting/`NotificationScheduler` job) - this
 * composable only renders the ask and reports which button was tapped, per code-conventions §7/§8.
 *
 * Uses [AlertDialog], the dialog primitive already established by `AddToListDialog` elsewhere in
 * this codebase, rather than introducing `ModalBottomSheet` as a new one just for this prompt.
 */
@Composable
internal fun NotificationOptInDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier =
                    Modifier
                        .size(NotificationOptInDialogConstant.ICON_BADGE_SIZE)
                        .clip(RoundedCornerShape(NotificationOptInDialogConstant.ICON_BADGE_CORNER_RADIUS)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(NotificationOptInDialogConstant.ICON_SIZE),
                )
            }
        },
        title = { Text(title) },
        text = {
            Column {
                Text(body)
                Text(
                    text = stringResource(Res.string.notification_opt_in_fineprint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.notification_opt_in_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.notification_opt_in_dismiss))
            }
        },
    )
}
