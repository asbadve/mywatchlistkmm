package com.ajinkyabadve.kmmmywatchlist

import androidx.compose.runtime.Composable

@Composable
expect fun ConfigureDialogWindow()

expect fun getDialogProperties(
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
): androidx.compose.ui.window.DialogProperties
