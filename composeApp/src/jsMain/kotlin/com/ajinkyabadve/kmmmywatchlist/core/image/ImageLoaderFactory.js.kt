package com.ajinkyabadve.kmmmywatchlist.core.image

import coil3.PlatformContext
import okio.Path

// The browser has no filesystem-backed disk cache; only the in-memory cache applies here.
actual fun imageCacheDir(context: PlatformContext): Path? = null
