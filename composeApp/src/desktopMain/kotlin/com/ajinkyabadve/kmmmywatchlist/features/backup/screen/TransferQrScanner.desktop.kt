package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Never shown: desktop sends transfers but doesn't receive them (no camera scanning). */
@Composable
internal actual fun TransferQrScanner(
    onCodeScanned: (String) -> Unit,
    permissionDenied: @Composable () -> Unit,
    modifier: Modifier,
) = Unit
