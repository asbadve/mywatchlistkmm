package com.ajinkyabadve.kmmmywatchlist.features.backup.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import kotlinx.serialization.Serializable

/**
 * One nullable field per allow-listed setting, so "absent from the file" and "set to false" stay
 * different: a restore only writes the settings the file actually has, and a backup made before a
 * setting existed never resets it to a default.
 */
@Serializable
data class BackupSettings(
    val selectedRegion: String? = null,
    val fallbackRegion: String? = null,
    val restrictedMode: Boolean? = null,
    val trendingAutoScroll: Boolean? = null,
    val episodeNotifications: Boolean? = null,
    val episodeAlertPromptSeen: Boolean? = null,
    val discoverMovieFilters: DiscoverFilters? = null,
    val discoverTvFilters: DiscoverFilters? = null,
)
