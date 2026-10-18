package com.ajinkyabadve.kmmmywatchlist.core.ui

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry

private const val CLIP_LABEL = "MyWatchList"

actual fun plainTextClipEntry(text: String): ClipEntry = ClipData.newPlainText(CLIP_LABEL, text).toClipEntry()
