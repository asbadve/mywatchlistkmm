package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface DiscoverFilterRepository {
    fun getSelectedMovieFilters(): DiscoverFilters

    fun setSelectedMovieFilters(filters: DiscoverFilters)

    fun getSelectedTvFilters(): DiscoverFilters

    fun setSelectedTvFilters(filters: DiscoverFilters)
}

/**
 * Persists the *last-applied* Discover filter set per media type, same `multiplatform-settings`
 * store `RestrictedModeRepository`/`RegionRepository`/auth already use, so a choice survives
 * navigating away or restarting the app. Falls back to "last year, popularity descending, no
 * genre/keyword restriction" when nothing has been applied yet.
 */
class DiscoverFilterRepositoryImpl(
    private val settings: Settings = com.ajinkyabadve.kmmmywatchlist.createSettings(),
) : DiscoverFilterRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override fun getSelectedMovieFilters(): DiscoverFilters = getFilters(DiscoverFilterConstant.KEY_MOVIE_FILTERS)

    override fun setSelectedMovieFilters(filters: DiscoverFilters) = setFilters(DiscoverFilterConstant.KEY_MOVIE_FILTERS, filters)

    override fun getSelectedTvFilters(): DiscoverFilters = getFilters(DiscoverFilterConstant.KEY_TV_FILTERS)

    override fun setSelectedTvFilters(filters: DiscoverFilters) = setFilters(DiscoverFilterConstant.KEY_TV_FILTERS, filters)

    private fun getFilters(key: String): DiscoverFilters {
        val storedJson = settings.getString(key, "")
        if (storedJson.isEmpty()) return defaultFilters()
        return try {
            json.decodeFromString(DiscoverFilters.serializer(), storedJson)
        } catch (e: SerializationException) {
            Napier.w(tag = TAG, throwable = e) { "Corrupted cached filters JSON, using default" }
            defaultFilters()
        } catch (e: IllegalArgumentException) {
            Napier.w(tag = TAG, throwable = e) { "Cached filters schema mismatch, using default" }
            defaultFilters()
        }
    }

    private fun setFilters(
        key: String,
        filters: DiscoverFilters,
    ) {
        settings.putString(key, json.encodeToString(DiscoverFilters.serializer(), filters))
    }

    private fun defaultFilters(): DiscoverFilters {
        val lastYear = Clock.System.todayIn(TimeZone.currentSystemDefault()).year - 1
        return DiscoverFilters(year = lastYear)
    }

    private companion object {
        const val TAG = "DiscoverFilterRepositoryImpl"
    }
}

private object DiscoverFilterConstant {
    const val KEY_MOVIE_FILTERS = "discover_movie_filters_json"
    const val KEY_TV_FILTERS = "discover_tv_filters_json"
}
