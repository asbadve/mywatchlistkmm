package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.ui.platform.ClipEntry

actual fun plainTextClipEntry(text: String): ClipEntry = ClipEntry.withPlainText(text)
