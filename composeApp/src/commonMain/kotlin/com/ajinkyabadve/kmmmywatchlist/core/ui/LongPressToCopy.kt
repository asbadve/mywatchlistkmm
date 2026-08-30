package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString

/**
 * Long-press-to-copy for a detail screen's title. Copies [text] to the system clipboard and gives
 * a haptic tick as the only feedback - no toast/snackbar mechanism exists anywhere in this app
 * yet, and adding one app-wide is out of scope for a copy-the-title convenience.
 *
 * Uses [LocalClipboardManager] (`ClipboardManager.setText`), not Compose 1.11.1's newer suspend
 * `LocalClipboard`/`ClipEntry` API: `ClipEntry` construction is platform-native (an AWT
 * `Transferable` on desktop, `ClipData` on Android, ...) with no plain-text convenience
 * constructor shared across platforms, which would mean a new `expect`/`actual` per platform just
 * to copy a string. `ClipboardManager` is `@Deprecated` in this version but still fully functional
 * and already multiplatform - revisit if it's ever actually removed.
 */
@Composable
fun Modifier.longPressToCopy(text: String): Modifier {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current
    return this.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
        onLongClick = {
            clipboardManager.setText(AnnotatedString(text))
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        },
    )
}
