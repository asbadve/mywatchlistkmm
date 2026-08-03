package com.ajinkyabadve.kmmmywatchlist.core.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor2.KtorNetworkFetcherFactory
import okio.Path

// In-memory cache: instant re-display of recently shown posters/backdrops while scrolling and
// switching tabs within a session. 25% of the app's available memory is Coil's recommended budget.
private const val MEMORY_CACHE_PERCENT = 0.25

// On-disk cache budget. This is the one that matters on slow/3G networks: images survive tab
// switches, process death, and app relaunches, so a poster is downloaded once and never again
// until evicted. 256 MB comfortably holds a browsing session's worth of TMDB imagery.
private const val MAX_DISK_CACHE_BYTES = 256L * 1024 * 1024

/**
 * Per-platform persistent directory for Coil's disk cache. Returns null on platforms without a
 * filesystem-backed cache (JS/browser), where only the in-memory cache applies.
 *
 * Persistent (not the OS temp dir) on purpose: a temp-dir cache can be purged between launches,
 * which on a slow connection means re-downloading everything the user already fetched.
 */
expect fun imageCacheDir(context: PlatformContext): Path?

/**
 * Single source of truth for the app's [ImageLoader]. Wires the Ktor network fetcher plus explicit
 * memory and (where available) persistent disk caches, instead of leaning on Coil's platform
 * defaults - the defaults fall back to the OS temp directory on desktop/iOS, which isn't durable.
 */
fun newImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader
        .Builder(context)
        .components {
            add(KtorNetworkFetcherFactory())
        }.memoryCache {
            MemoryCache
                .Builder()
                .maxSizePercent(context, MEMORY_CACHE_PERCENT)
                .build()
        }.apply {
            imageCacheDir(context)?.let { dir ->
                diskCache {
                    DiskCache
                        .Builder()
                        .directory(dir)
                        .maxSizeBytes(MAX_DISK_CACHE_BYTES)
                        .build()
                }
            }
        }.build()
