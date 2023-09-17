package com.ajinkyabadve.kmmmywatchlist

import com.ajinkyabadve.kmmmywatchlist.util.commonConfig
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.component.setupDefaultComponents
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.http.HttpHeaders
import kotlinx.atomicfu.TraceBase.None.append
import kotlinx.browser.window
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem

internal actual fun openUrl(url: String?) {
    url?.let { window.open(it) }
}

