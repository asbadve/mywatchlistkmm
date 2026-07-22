package com.ajinkyabadve.kmmmywatchlist

import androidx.compose.runtime.Composable

@Composable
actual fun ConfigureDialogWindow() {}

actual fun getDialogProperties(
    dismissOnBackPress: Boolean,
    dismissOnClickOutside: Boolean,
): androidx.compose.ui.window.DialogProperties =
    androidx.compose.ui.window.DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
    )
