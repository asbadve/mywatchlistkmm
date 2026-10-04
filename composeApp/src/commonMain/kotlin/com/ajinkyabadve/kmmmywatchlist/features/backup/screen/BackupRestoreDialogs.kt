package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.core.file.BackupFileLauncher
import kotlinx.coroutines.launch
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_cancel
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.backup_dialog_message
import mywatchlist.composeapp.generated.resources.backup_dialog_title
import mywatchlist.composeapp.generated.resources.backup_error_malformed
import mywatchlist.composeapp.generated.resources.backup_error_unsupported_format
import mywatchlist.composeapp.generated.resources.backup_export_action
import mywatchlist.composeapp.generated.resources.backup_export_failed
import mywatchlist.composeapp.generated.resources.backup_export_success
import mywatchlist.composeapp.generated.resources.backup_restore_action
import mywatchlist.composeapp.generated.resources.backup_restore_confirm_action
import mywatchlist.composeapp.generated.resources.backup_restore_confirm_message
import mywatchlist.composeapp.generated.resources.backup_restore_confirm_title
import mywatchlist.composeapp.generated.resources.backup_restore_result
import mywatchlist.composeapp.generated.resources.transfer_receive_action
import mywatchlist.composeapp.generated.resources.transfer_send_action
import org.jetbrains.compose.resources.stringResource

/**
 * Every dialog of the backup flow, driven by [BackupScreenModel.uiState]. Renders nothing while
 * [BackupUiState.Idle]. [onRestored] runs once after a restore so the caller can re-read the
 * settings it shows (region, restricted mode, ...), which the restore may have just changed.
 */
@Composable
fun BackupRestoreDialogs(
    screenModel: BackupScreenModel,
    fileLauncher: BackupFileLauncher,
    onRestored: () -> Unit,
    // Parameters, not direct calls, so the desktop UI test can render either set of buttons.
    canSendToDevice: Boolean = supportsTransferSending(),
    canReceiveFromDevice: Boolean = supportsTransferReceiving(),
    transferDialog: @Composable (
        mode: DeviceTransferMode,
        onBackupReceived: (String) -> Unit,
        onDismiss: () -> Unit,
    ) -> Unit = { mode, onReceived, onDismiss ->
        DeviceTransferDialog(mode, onReceived, onDismiss)
    },
) {
    val state by screenModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var transferMode by remember { mutableStateOf<DeviceTransferMode?>(null) }

    transferMode?.let { mode ->
        transferDialog(
            mode,
            { backupJson ->
                transferMode = null
                // Same path as a picked file: validate, then ask before restoring.
                screenModel.onFilePicked(backupJson)
            },
            { transferMode = null },
        )
    }

    when (val current = state) {
        BackupUiState.Idle -> Unit

        BackupUiState.Choosing ->
            AlertDialog(
                onDismissRequest = screenModel::dismiss,
                title = { Text(stringResource(Res.string.backup_dialog_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(Res.string.backup_dialog_message))
                        Button(
                            onClick = {
                                scope.launch {
                                    val json = screenModel.buildExport()
                                    screenModel.onExportFinished(fileLauncher.save(screenModel.suggestedFileName(), json))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(Res.string.backup_export_action)) }
                        OutlinedButton(
                            onClick = { scope.launch { screenModel.onFilePicked(fileLauncher.open()) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(Res.string.backup_restore_action)) }
                        if (canSendToDevice) {
                            OutlinedButton(
                                onClick = {
                                    screenModel.dismiss()
                                    transferMode = DeviceTransferMode.SEND
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(Res.string.transfer_send_action)) }
                        }
                        if (canReceiveFromDevice) {
                            OutlinedButton(
                                onClick = {
                                    screenModel.dismiss()
                                    transferMode = DeviceTransferMode.RECEIVE
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(Res.string.transfer_receive_action)) }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = screenModel::dismiss) { Text(stringResource(Res.string.action_cancel)) } },
            )

        BackupUiState.Working ->
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(Res.string.backup_dialog_title)) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {},
            )

        is BackupUiState.ConfirmRestore ->
            AlertDialog(
                onDismissRequest = screenModel::dismiss,
                title = { Text(stringResource(Res.string.backup_restore_confirm_title)) },
                text = {
                    val summary = current.summary
                    Text(
                        stringResource(
                            Res.string.backup_restore_confirm_message,
                            summary.people,
                            summary.collections,
                            summary.reminders,
                            summary.settings,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = screenModel::confirmRestore) { Text(stringResource(Res.string.backup_restore_confirm_action)) }
                },
                dismissButton = { TextButton(onClick = screenModel::dismiss) { Text(stringResource(Res.string.action_cancel)) } },
            )

        is BackupUiState.Restored -> {
            LaunchedEffect(current) { onRestored() }
            val outcome = current.outcome
            BackupMessageDialog(
                message =
                    stringResource(
                        Res.string.backup_restore_result,
                        outcome.peopleAdded,
                        outcome.collectionsAdded,
                        outcome.remindersAdded,
                        outcome.settingsApplied,
                    ),
                onDismiss = screenModel::dismiss,
            )
        }

        BackupUiState.Exported -> BackupMessageDialog(stringResource(Res.string.backup_export_success), screenModel::dismiss)

        BackupUiState.ExportFailed -> BackupMessageDialog(stringResource(Res.string.backup_export_failed), screenModel::dismiss)

        is BackupUiState.UnsupportedFormat ->
            BackupMessageDialog(
                stringResource(Res.string.backup_error_unsupported_format, current.found, current.supported),
                screenModel::dismiss,
            )

        BackupUiState.Malformed -> BackupMessageDialog(stringResource(Res.string.backup_error_malformed), screenModel::dismiss)
    }
}

@Composable
private fun BackupMessageDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.backup_dialog_title)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) } },
    )
}
