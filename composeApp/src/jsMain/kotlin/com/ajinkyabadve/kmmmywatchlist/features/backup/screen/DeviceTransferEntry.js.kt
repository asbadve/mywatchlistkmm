package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.compose.runtime.Composable

actual fun supportsTransferSending(): Boolean = false

actual fun supportsTransferReceiving(): Boolean = false

/** Never shown: backup is hidden on the web target (item 15). */
@Composable
actual fun DeviceTransferDialog(
    mode: DeviceTransferMode,
    onBackupReceived: (String) -> Unit,
    onDismiss: () -> Unit,
) = Unit
