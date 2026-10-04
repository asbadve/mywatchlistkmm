package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.core.notification.PendingReminder
import kotlinx.datetime.LocalTime
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_cancel
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.action_confirm
import mywatchlist.composeapp.generated.resources.debug_pending_reminder_not_registered
import mywatchlist.composeapp.generated.resources.debug_pending_reminders_empty
import mywatchlist.composeapp.generated.resources.debug_pending_reminders_title
import mywatchlist.composeapp.generated.resources.settings_reminder_time_dialog_title
import org.jetbrains.compose.resources.stringResource

private object ReleaseReminderDialogsConstant {
    val PENDING_LIST_MAX_HEIGHT = 360.dp
    val PENDING_ROW_SPACING = 6.dp
}

/** Material3's own time picker in its dialog - the platform component for exactly this. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderTimePickerDialog(
    initialTime: LocalTime,
    is24Hour: Boolean,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute, is24Hour = is24Hour)
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_reminder_time_dialog_title)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime(state.hour, state.minute)) }) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    ) {
        TimePicker(state = state)
    }
}

/** Debug-only: what the OS currently holds for release reminders (see the "Show pending
 *  reminders" row). Android can't list alarms, so an entry marked "not registered" is one the
 *  database expects but the system no longer has - the case the boot receiver exists to repair. */
@Composable
internal fun PendingRemindersDialog(
    pending: List<PendingReminder>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.debug_pending_reminders_title, pending.size)) },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(
                            max = ReleaseReminderDialogsConstant.PENDING_LIST_MAX_HEIGHT,
                        ).verticalScroll(rememberScrollState()),
            ) {
                if (pending.isEmpty()) {
                    Text(stringResource(Res.string.debug_pending_reminders_empty))
                }
                val notRegistered = stringResource(Res.string.debug_pending_reminder_not_registered)
                pending.forEach { reminder ->
                    Text(
                        text =
                            buildString {
                                append(reminder.id)
                                append("\n")
                                append(reminder.fireAt?.toString().orEmpty())
                                if (!reminder.isRegistered) append(" - ").append(notRegistered)
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (reminder.isRegistered) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = ReleaseReminderDialogsConstant.PENDING_ROW_SPACING),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}

/** Debug-only: a one-line result message (e.g. when the test reminder will fire). */
@Composable
internal fun DebugMessageDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
    )
}
