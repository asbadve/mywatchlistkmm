package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Camera preview that reports every QR code it reads (item 15 stage 2). QRKit on Android/iOS;
 * desktop never receives a transfer (`supportsTransferReceiving()` is false there), so its version
 * is never shown. [permissionDenied] renders in place of the preview when camera access is refused.
 */
@Composable
internal expect fun TransferQrScanner(
    onCodeScanned: (String) -> Unit,
    permissionDenied: @Composable () -> Unit,
    modifier: Modifier = Modifier,
)
