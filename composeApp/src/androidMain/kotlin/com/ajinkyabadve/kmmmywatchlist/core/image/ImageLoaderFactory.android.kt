package com.ajinkyabadve.kmmmywatchlist.core.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

// PlatformContext is android.content.Context on Android, so cacheDir is the app's private cache
// directory - not backed up, cleared on uninstall or "Clear cache", exactly right for regenerable
// image data.
actual fun imageCacheDir(context: PlatformContext): Path? = context.cacheDir.resolve("image_cache").toOkioPath()
