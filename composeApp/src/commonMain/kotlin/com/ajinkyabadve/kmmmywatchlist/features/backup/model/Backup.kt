package com.ajinkyabadve.kmmmywatchlist.features.backup.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import kotlinx.serialization.Serializable

/**
 * The backup file (future_features_checklist.md item 15): everything this app keeps *only* on the
 * device, and nothing TMDB gives back on sign-in. See `BackupRepository`'s KDoc for what is
 * deliberately left out - above all, the TMDB session.
 *
 * [format] is the backup contract's own version and is deliberately **not**
 * `LocalSchemaVersion.CURRENT`: a database migration that doesn't change what is exported must not
 * invalidate old backups, and a change to this shape must bump [format] even when the schema is
 * untouched. Bump it only for a change an older build would misread; adding an optional field
 * doesn't need it, since older builds decode with `ignoreUnknownKeys`.
 */
@Serializable
data class BackupEnvelope(
    val format: Int,
    val exportedAt: Long,
    val people: List<BackupPerson> = emptyList(),
    val collections: List<BackupCollection> = emptyList(),
    val reminders: List<BackupReminder> = emptyList(),
    val reminderPreference: BackupReminderPreference? = null,
    val settings: BackupSettings = BackupSettings(),
)

@Serializable
data class BackupPerson(
    val id: Long,
    val name: String,
    val profilePath: String? = null,
    val addedAt: Long,
)

@Serializable
data class BackupCollection(
    val id: Long,
    val name: String,
    val posterPath: String? = null,
    val addedAt: Long,
)

/** [seasonNumber]/[episodeNumber] are both null for a movie or show reminder, both set for an
 *  episode reminder. [releaseDate] is `yyyy-MM-dd`. */
@Serializable
data class BackupReminder(
    val mediaId: Long,
    val mediaType: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val title: String,
    val posterPath: String? = null,
    val releaseDate: String,
    val releaseSource: String? = null,
)

@Serializable
data class BackupReminderPreference(
    val enabled: Boolean,
    val minutesOfDay: Int,
)

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
    val episodeNotifications: Boolean? = null,
    val episodeAlertPromptSeen: Boolean? = null,
    val discoverMovieFilters: DiscoverFilters? = null,
    val discoverTvFilters: DiscoverFilters? = null,
)
