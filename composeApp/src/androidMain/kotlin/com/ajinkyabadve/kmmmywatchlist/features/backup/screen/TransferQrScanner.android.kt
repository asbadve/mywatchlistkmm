package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import qrscanner.CameraLens
import qrscanner.QrScanner

@Composable
internal actual fun TransferQrScanner(
    onCodeScanned: (String) -> Unit,
    permissionDenied: @Composable () -> Unit,
    modifier: Modifier,
) {
    QrScanner(
        modifier = modifier,
        flashlightOn = false,
        cameraLens = CameraLens.Back,
        openImagePicker = false,
        onCompletion = onCodeScanned,
        imagePickerHandler = {},
        // A frame that doesn't decode isn't an error worth showing; the camera just keeps looking.
        onFailure = {},
        permissionDeniedView = permissionDenied,
    )
}
