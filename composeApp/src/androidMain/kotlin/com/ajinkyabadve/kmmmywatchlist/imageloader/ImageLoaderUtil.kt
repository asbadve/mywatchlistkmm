package com.ajinkyabadve.kmmmywatchlist.imageloader

import com.ajinkyabadve.kmmmywatchlist.AndroidApp
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.cache.memory.maxSizePercent
import com.seiko.imageloader.component.setupDefaultComponents
import com.seiko.imageloader.defaultImageResultMemoryCache
import com.seiko.imageloader.option.androidContext
import okio.Path.Companion.toOkioPath

internal actual fun generateImageLoader(): ImageLoader {
    return ImageLoader {
        options {
            androidContext(AndroidApp.INSTANCE.applicationContext)
        }
        components {
            setupDefaultComponents()
        }
        interceptor {
            // cache 100 success image result, without bitmap
            defaultImageResultMemoryCache()
            memoryCacheConfig {
                // Set the max size to 25% of the app's available memory.
                maxSizePercent(AndroidApp.INSTANCE.applicationContext, 0.25)
            }
            diskCacheConfig {
                directory(
                    AndroidApp.INSTANCE.applicationContext.cacheDir.resolve("image_cache")
                        .toOkioPath(),
                )
                maxSizeBytes(512L * 1024 * 1024) // 512MB
            }
        }
    }
}