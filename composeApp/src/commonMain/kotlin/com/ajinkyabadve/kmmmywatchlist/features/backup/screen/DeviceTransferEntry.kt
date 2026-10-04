package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.compose.runtime.Composable

enum class DeviceTransferMode {
    SEND,
    RECEIVE,
}

/**
 * The shared-code view of item 15 stage 2: whether this platform can send / receive a backup over
 * the local network, and the dialog that does it. The real implementation lives in the non-web
 * source set (QR codes, camera, sockets); the web target, which has no backup at all, gets false
 * and an empty dialog.
 */
expect fun supportsTransferSending(): Boolean

/** Phones scan or type the code; desktop types it. */
expect fun supportsTransferReceiving(): Boolean

/** [onBackupReceived] gets the received backup JSON, which goes through the same
 *  confirm-and-restore as a file. */
@Composable
expect fun DeviceTransferDialog(
    mode: DeviceTransferMode,
    onBackupReceived: (String) -> Unit,
    onDismiss: () -> Unit,
)
