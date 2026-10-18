package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.ui.platform.ClipEntry

/**
 * A [ClipEntry] holding [text] as plain text, for `LocalClipboard.current.setClipEntry(...)`.
 *
 * Platform equivalent checked (Compose Multiplatform 1.11.1): `LocalClipboard` replaced the
 * deprecated `LocalClipboardManager.setText(AnnotatedString)`, but `ClipEntry` is an `expect class`
 * with no common plain-text factory - iOS and web have `ClipEntry.withPlainText`, desktop wraps an
 * AWT `Transferable`, Android a `ClipData`. Hence one tiny actual per platform.
 */
expect fun plainTextClipEntry(text: String): ClipEntry
