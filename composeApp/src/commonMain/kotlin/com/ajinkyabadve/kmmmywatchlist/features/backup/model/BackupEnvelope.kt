package com.ajinkyabadve.kmmmywatchlist.features.backup.model

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
