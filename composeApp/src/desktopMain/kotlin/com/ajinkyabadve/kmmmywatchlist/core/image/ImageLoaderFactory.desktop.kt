package com.ajinkyabadve.kmmmywatchlist.core.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath
import java.io.File

// A persistent per-user location that survives relaunch, unlike Coil's JVM default of the system
// temp directory. Coil's DiskCache creates the directory on first use, so no mkdirs here.
actual fun imageCacheDir(context: PlatformContext): Path? = File(System.getProperty("user.home"), ".mywatchlist/image_cache").toOkioPath()
