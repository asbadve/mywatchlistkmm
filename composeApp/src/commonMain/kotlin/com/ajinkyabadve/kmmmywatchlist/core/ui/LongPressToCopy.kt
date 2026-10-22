package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch

/**
 * Long-press-to-copy for a detail screen's title. Copies [text] to the system clipboard and gives
 * a haptic tick as the only feedback - no toast/snackbar mechanism exists anywhere in this app
 * yet, and adding one app-wide is out of scope for a copy-the-title convenience.
 *
 * Writes through [LocalClipboard] - the suspend API that replaced the deprecated
 * `LocalClipboardManager` - with the clip built by [plainTextClipEntry], since `ClipEntry` has no
 * shared plain-text constructor across platforms.
 */
@Composable
fun Modifier.longPressToCopy(text: String): Modifier {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    return this.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
        onLongClick = {
            // LocalClipboard's write is suspend (it replaced the blocking LocalClipboardManager).
            coroutineScope.launch { clipboard.setClipEntry(plainTextClipEntry(text)) }
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        },
    )
}
