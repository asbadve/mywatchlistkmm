package com.ajinkyabadve.kmmmywatchlist.core.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

// The iOS Caches directory: OS-managed, persists across launches, and is purgeable under storage
// pressure - the standard home for a regenerable cache.
actual fun imageCacheDir(context: PlatformContext): Path? {
    val caches =
        NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: return null
    return "$caches/image_cache".toPath()
}
