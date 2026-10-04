package com.ajinkyabadve.kmmmywatchlist.features.trending.repository

import com.ajinkyabadve.kmmmywatchlist.createSettings
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.Trailer
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.TrailerSource
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * On-device cache of the Latest Trailers rail, per source. Building one source costs ~11 TMDB
 * requests (its list plus a videos call per title - TMDB has no list+videos endpoint), so a fresh
 * cache means reopening the Trending tab or switching back to a chip costs nothing. Trailer lists
 * change slowly; [TrailerCacheConstant.TTL_MILLIS] bounds how stale they can get.
 *
 * Stored as JSON in [Settings], not a database table: it's a few KB, read whole, and needs no
 * schema migration. Not in the backup allow-list (`BackupSettings`) - it's a cache.
 */
interface TrailerCacheRepository {
    /** The cached trailers for [source], or null when there are none or they are older than the TTL. */
    fun get(source: TrailerSource): List<Trailer>?

    fun put(
        source: TrailerSource,
        trailers: List<Trailer>,
    )
}

class TrailerCacheRepositoryImpl(
    private val settings: Settings = createSettings(),
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : TrailerCacheRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override fun get(source: TrailerSource): List<Trailer>? {
        val stored = settings.getStringOrNull(source.cacheKey()) ?: return null
        val cached =
            try {
                json.decodeFromString(CachedTrailers.serializer(), stored)
            } catch (e: SerializationException) {
                return null
            } catch (e: IllegalArgumentException) {
                return null
            }
        val age = nowMillis() - cached.savedAtMillis
        // A negative age means the clock went backwards; treat that as stale rather than trusting it.
        return cached.trailers.takeIf { age in 0 until TrailerCacheConstant.TTL_MILLIS }
    }

    override fun put(
        source: TrailerSource,
        trailers: List<Trailer>,
    ) {
        settings.putString(source.cacheKey(), json.encodeToString(CachedTrailers.serializer(), CachedTrailers(nowMillis(), trailers)))
    }

    private fun TrailerSource.cacheKey(): String = TrailerCacheConstant.KEY_PREFIX + name.lowercase()
}

@Serializable
private data class CachedTrailers(
    val savedAtMillis: Long,
    val trailers: List<Trailer>,
)

internal object TrailerCacheConstant {
    const val KEY_PREFIX = "trailer_cache_"

    /** 12 hours - agreed 2026-10-04 as the freshness/requests trade-off. */
    const val TTL_MILLIS = 12L * 60 * 60 * 1000
}
