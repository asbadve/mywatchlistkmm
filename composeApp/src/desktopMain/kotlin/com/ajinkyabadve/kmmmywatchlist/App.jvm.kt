package com.ajinkyabadve.kmmmywatchlist

import okio.Path.Companion.toOkioPath
import java.awt.Desktop
import java.io.File
import java.net.URI

internal actual fun openUrl(url: String?) {
    val uri = url?.let { URI.create(it) } ?: return
    Desktop.getDesktop().browse(uri)
}
