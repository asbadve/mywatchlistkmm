package com.ajinkyabadve.kmmmywatchlist.features.backup.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.constant.NotificationSettingsConstant
import com.ajinkyabadve.kmmmywatchlist.core.constant.RegionConstant
import com.ajinkyabadve.kmmmywatchlist.core.constant.RestrictedModeConstant
import com.ajinkyabadve.kmmmywatchlist.createSettings
import com.ajinkyabadve.kmmmywatchlist.features.backup.model.BackupCollection
import com.ajinkyabadve.kmmmywatchlist.features.backup.model.BackupEnvelope
import com.ajinkyabadve.kmmmywatchlist.features.backup.model.BackupPerson
import com.ajinkyabadve.kmmmywatchlist.features.backup.model.BackupReminder
import com.ajinkyabadve.kmmmywatchlist.features.backup.model.BackupReminderPreference
import com.ajinkyabadve.kmmmywatchlist.features.backup.model.BackupSettings
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.DiscoverFilterConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRecord
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.parseDateOrNull
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderKey
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderPreference
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.toLocalTime
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.toMinutesOfDay
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRecord
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepositoryImpl
import com.russhwolf.settings.Settings
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object BackupConstant {
    /** The backup contract's version - see [BackupEnvelope]'s KDoc for why it isn't the DB schema version. */
    const val FORMAT = 1
    const val FILE_NAME_PREFIX = "mywatchlist-backup-"
    const val FILE_EXTENSION = ".json"
    const val MIME_TYPE = "application/json"
    internal const val FORMAT_FIELD = "format"
    internal const val MINUTES_PER_DAY = 24 * 60
    internal val REGION_CODE = Regex("^[A-Z]{2}$")
}

/** How many of each thing a backup file holds - shown in the restore confirmation. */
data class BackupSummary(
    val people: Int,
    val collections: Int,
    val reminders: Int,
    val settings: Int,
)

sealed interface BackupDecodeResult {
    data class Valid(
        val envelope: BackupEnvelope,
        val summary: BackupSummary,
    ) : BackupDecodeResult

    /** Written by a newer build than this one. Refused outright - half-restoring a format this
     *  build doesn't understand is worse than declining. */
    data class UnsupportedFormat(
        val found: Int,
        val supported: Int,
    ) : BackupDecodeResult

    /** Not a backup file, or a hand-edited one with an invalid field. Nothing is written. */
    data object Malformed : BackupDecodeResult
}

/** What a restore actually added - lower than [BackupSummary]'s counts when some items were
 *  already on this device (restore merges, it never overwrites). */
data class RestoreOutcome(
    val peopleAdded: Int,
    val collectionsAdded: Int,
    val remindersAdded: Int,
    val settingsApplied: Int,
)

/**
 * Export and restore of the device-only data (item 15). Composes the existing repositories rather
 * than reading SQLDelight directly, like every other repository here.
 *
 * Left out of the file, on purpose - don't "fix" these:
 * - **The TMDB session (`auth_*` keys).** It is a bearer credential for the user's real account,
 *   and backup files get copied into cloud drives, chats and email. Settings are exported from an
 *   explicit allow-list ([BackupSettings]), never by dumping the whole [Settings] store, so a future
 *   key can't join the export by accident.
 * - **Poll state** (`lastKnownCreditIds` / `lastKnownPartIds`, the notification ledger). Restored
 *   follows start un-polled, so the next poll only baselines and fires nothing - writing *less*
 *   is what keeps a restore quiet.
 * - **The privacy-consent flag.** A user-editable file must not let a fresh install skip consent.
 * - **Favorites, watchlist, lists and caches.** TMDB returns them on sign-in.
 */
interface BackupRepository {
    suspend fun exportToJson(): String

    /** Parses and validates a file without writing anything, for the confirmation step. */
    fun decode(json: String): BackupDecodeResult

    /** Merges a validated backup into this device's data. Only the settings present in the file
     *  are written. */
    suspend fun restore(envelope: BackupEnvelope): RestoreOutcome
}

class BackupRepositoryImpl(
    private val favoritePersonRepository: FavoritePersonRepository = FavoritePersonRepositoryImpl(),
    private val favoriteCollectionRepository: FavoriteCollectionRepository = FavoriteCollectionRepositoryImpl(),
    private val releaseReminderRepository: ReleaseReminderRepository = ReleaseReminderRepositoryImpl(),
    private val settings: Settings = createSettings(),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : BackupRepository {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            explicitNulls = false
        }

    override suspend fun exportToJson(): String {
        val preference = releaseReminderRepository.preference()
        val envelope =
            BackupEnvelope(
                format = BackupConstant.FORMAT,
                exportedAt = now(),
                people =
                    favoritePersonRepository.allForBackup().map {
                        BackupPerson(id = it.id, name = it.name, profilePath = it.profilePath, addedAt = it.addedAt)
                    },
                collections =
                    favoriteCollectionRepository.allForBackup().map {
                        BackupCollection(id = it.id, name = it.name, posterPath = it.posterPath, addedAt = it.addedAt)
                    },
                reminders = releaseReminderRepository.allReminders().map { it.toBackup() },
                reminderPreference =
                    BackupReminderPreference(enabled = preference.enabled, minutesOfDay = preference.time.toMinutesOfDay()),
                settings = exportSettings(),
            )
        return json.encodeToString(BackupEnvelope.serializer(), envelope)
    }

    override fun decode(json: String): BackupDecodeResult {
        try {
            // Read the format first, on its own: a newer file may not decode as this build's
            // envelope at all, and it should say "made by a newer version", not "not a backup".
            val format =
                this.json
                    .parseToJsonElement(json)
                    .jsonObject[BackupConstant.FORMAT_FIELD]
                    ?.jsonPrimitive
                    ?.intOrNull ?: return BackupDecodeResult.Malformed
            if (format > BackupConstant.FORMAT) return BackupDecodeResult.UnsupportedFormat(format, BackupConstant.FORMAT)
            val envelope = this.json.decodeFromString(BackupEnvelope.serializer(), json)
            if (!envelope.isValid()) return BackupDecodeResult.Malformed
            return BackupDecodeResult.Valid(envelope, envelope.summary())
        } catch (e: SerializationException) {
            return BackupDecodeResult.Malformed
        } catch (e: IllegalArgumentException) {
            // jsonObject on a non-object root, and jsonPrimitive on a non-primitive field, throw this.
            return BackupDecodeResult.Malformed
        }
    }

    override suspend fun restore(envelope: BackupEnvelope): RestoreOutcome {
        val peopleAdded =
            favoritePersonRepository.restore(
                envelope.people.map {
                    FavoritePersonRecord(
                        id = it.id,
                        name = it.name,
                        profilePath = it.profilePath,
                        addedAt = it.addedAt,
                    )
                },
            )
        val collectionsAdded =
            favoriteCollectionRepository.restore(
                envelope.collections.map {
                    FavoriteCollectionRecord(id = it.id, name = it.name, posterPath = it.posterPath, addedAt = it.addedAt)
                },
            )
        val remindersAdded = releaseReminderRepository.restore(envelope.reminders.map { it.toReleaseReminder() })
        var settingsApplied = restoreSettings(envelope.settings)
        envelope.reminderPreference?.let {
            releaseReminderRepository.setPreference(ReminderPreference(enabled = it.enabled, time = it.minutesOfDay.toLocalTime()))
            settingsApplied++
        }
        return RestoreOutcome(peopleAdded, collectionsAdded, remindersAdded, settingsApplied)
    }

    /** Only keys actually stored are exported - an unset key stays absent, so restoring this file
     *  elsewhere never pushes this device's *defaults* onto the other one. */
    private fun exportSettings(): BackupSettings =
        BackupSettings(
            selectedRegion = stringIfSet(RegionConstant.KEY_SELECTED_REGION),
            fallbackRegion = stringIfSet(RegionConstant.KEY_FALLBACK_REGION),
            restrictedMode = booleanIfSet(RestrictedModeConstant.KEY_RESTRICTED_MODE_ENABLED),
            episodeNotifications = booleanIfSet(NotificationSettingsConstant.KEY_EPISODE_NOTIFICATIONS_ENABLED),
            episodeAlertPromptSeen = booleanIfSet(NotificationSettingsConstant.KEY_EPISODE_ALERT_OPT_IN_PROMPT_SEEN),
            discoverMovieFilters = filtersIfSet(DiscoverFilterConstant.KEY_MOVIE_FILTERS),
            discoverTvFilters = filtersIfSet(DiscoverFilterConstant.KEY_TV_FILTERS),
        )

    private fun restoreSettings(backup: BackupSettings): Int {
        var applied = 0
        backup.selectedRegion?.let {
            settings.putString(RegionConstant.KEY_SELECTED_REGION, it)
            applied++
        }
        backup.fallbackRegion?.let {
            settings.putString(RegionConstant.KEY_FALLBACK_REGION, it)
            applied++
        }
        backup.restrictedMode?.let {
            settings.putBoolean(RestrictedModeConstant.KEY_RESTRICTED_MODE_ENABLED, it)
            applied++
        }
        backup.episodeNotifications?.let {
            settings.putBoolean(NotificationSettingsConstant.KEY_EPISODE_NOTIFICATIONS_ENABLED, it)
            applied++
        }
        backup.episodeAlertPromptSeen?.let {
            settings.putBoolean(NotificationSettingsConstant.KEY_EPISODE_ALERT_OPT_IN_PROMPT_SEEN, it)
            applied++
        }
        backup.discoverMovieFilters?.let {
            settings.putString(DiscoverFilterConstant.KEY_MOVIE_FILTERS, json.encodeToString(DiscoverFilters.serializer(), it))
            applied++
        }
        backup.discoverTvFilters?.let {
            settings.putString(DiscoverFilterConstant.KEY_TV_FILTERS, json.encodeToString(DiscoverFilters.serializer(), it))
            applied++
        }
        return applied
    }

    private fun stringIfSet(key: String): String? = if (settings.hasKey(key)) settings.getStringOrNull(key) else null

    private fun booleanIfSet(key: String): Boolean? = if (settings.hasKey(key)) settings.getBooleanOrNull(key) else null

    /** A corrupt stored filter is simply left out of the export, the same "fall back, don't fail"
     *  rule `DiscoverFilterRepositoryImpl` applies when reading it. */
    private fun filtersIfSet(key: String): DiscoverFilters? {
        val stored = stringIfSet(key)?.takeIf { it.isNotBlank() } ?: return null
        return try {
            json.decodeFromString(DiscoverFilters.serializer(), stored)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

/**
 * The file is user-editable by design, so every field is checked before anything is written - a
 * hand-edited backup fails cleanly instead of writing junk rows.
 */
private fun BackupEnvelope.isValid(): Boolean =
    people.all { it.id > 0 && it.name.isNotBlank() } &&
        collections.all { it.id > 0 && it.name.isNotBlank() } &&
        reminders.all { it.isValid() } &&
        (reminderPreference?.minutesOfDay?.let { it in 0 until BackupConstant.MINUTES_PER_DAY } ?: true) &&
        listOfNotNull(settings.selectedRegion, settings.fallbackRegion).all { BackupConstant.REGION_CODE.matches(it) }

private fun BackupReminder.isValid(): Boolean {
    val episodeFieldsConsistent = (seasonNumber == null) == (episodeNumber == null)
    val episodeNumbersValid = (seasonNumber ?: 0) >= 0 && (episodeNumber ?: 0) >= 0
    return mediaId > 0 &&
        mediaType in setOf(MediaTypeConstant.MOVIE, MediaTypeConstant.TV) &&
        title.isNotBlank() &&
        parseDateOrNull(releaseDate) != null &&
        episodeFieldsConsistent &&
        episodeNumbersValid
}

private fun BackupEnvelope.summary(): BackupSummary {
    val settingsCount =
        listOfNotNull(
            settings.selectedRegion,
            settings.fallbackRegion,
            settings.restrictedMode,
            settings.episodeNotifications,
            settings.episodeAlertPromptSeen,
            settings.discoverMovieFilters,
            settings.discoverTvFilters,
            reminderPreference,
        ).size
    return BackupSummary(people = people.size, collections = collections.size, reminders = reminders.size, settings = settingsCount)
}

private fun ReleaseReminder.toBackup(): BackupReminder =
    BackupReminder(
        mediaId = key.mediaId,
        mediaType = key.mediaType,
        seasonNumber = key.seasonNumber,
        episodeNumber = key.episodeNumber,
        title = title,
        posterPath = posterPath,
        releaseDate = releaseDate.toString(),
        releaseSource = releaseSource,
    )

// Only called after isValid(), so the date parses.
private fun BackupReminder.toReleaseReminder(): ReleaseReminder =
    ReleaseReminder(
        key = ReminderKey(mediaId = mediaId, mediaType = mediaType, seasonNumber = seasonNumber, episodeNumber = episodeNumber),
        title = title,
        posterPath = posterPath,
        releaseDate = requireNotNull(parseDateOrNull(releaseDate)),
        releaseSource = releaseSource,
    )
