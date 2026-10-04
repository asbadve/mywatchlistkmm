package com.ajinkyabadve.kmmmywatchlist.features.notifications.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import com.ajinkyabadve.kmmmywatchlist.db.ReleaseReminder as ReleaseReminderRow
import com.ajinkyabadve.kmmmywatchlist.db.ReminderPreference as ReminderPreferenceRow

internal object ReleaseReminderConstant {
    /** 09:00 local - most people are awake and past their morning routine, and a release-day
     *  reminder still leaves the whole day to plan a watch. */
    val DEFAULT_TIME = LocalTime(9, 0)
    const val DEFAULT_ENABLED = true

    /** A reminder row is kept this long after its release date, so a date that slips backwards
     *  (or a late re-poll) doesn't lose it, then aged out. */
    const val EXPIRY_DAYS = 7

    /** Stored in the season/episode columns for a reminder on the title itself - see the
     *  `releaseReminder` table comment for why this isn't NULL. */
    const val NO_EPISODE = -1L

    const val MINUTES_PER_HOUR = 60
}

/**
 * Identifies one reminder: a movie, a show (its premiere), or one episode of a show
 * ([seasonNumber] and [episodeNumber] both set).
 */
data class ReminderKey(
    val mediaId: Long,
    val mediaType: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
) {
    val isEpisode: Boolean get() = seasonNumber != null && episodeNumber != null
}

/** One stored release reminder. [releaseSource] says which date won (e.g. `theatrical:IN`). */
data class ReleaseReminder(
    val key: ReminderKey,
    val title: String,
    val posterPath: String?,
    val releaseDate: LocalDate,
    val releaseSource: String?,
)

/** The global release-reminder preferences: whether reminders are on, and when in the day they
 *  fire (local time). */
data class ReminderPreference(
    val enabled: Boolean,
    val time: LocalTime,
)

/**
 * Local-only store for checklist item 16's release reminders and their delivery preference. The
 * database is the source of truth - `ReleaseReminderCoordinator` re-derives every OS-scheduled
 * notification from it, which is what lets a reboot, a restore or a changed time only need a
 * reschedule-all.
 */
interface ReleaseReminderRepository {
    fun observeHasReminder(key: ReminderKey): Flow<Boolean>

    fun observeReminderKeys(): Flow<Set<ReminderKey>>

    suspend fun setReminder(
        reminder: ReleaseReminder,
        enabled: Boolean,
    )

    suspend fun allReminders(): List<ReleaseReminder>

    suspend fun updateReleaseDate(
        key: ReminderKey,
        releaseDate: LocalDate,
        releaseSource: String?,
    )

    /** Drops reminders whose release date is more than [ReleaseReminderConstant.EXPIRY_DAYS]
     *  before [today]. */
    suspend fun deleteExpired(today: LocalDate)

    suspend fun deleteAll()

    fun observePreference(): Flow<ReminderPreference>

    suspend fun preference(): ReminderPreference

    suspend fun setPreference(preference: ReminderPreference)
}

class ReleaseReminderRepositoryImpl(
    // Same test seam as every other repository in this codebase - see TrackedMediaRepositoryImpl's kdoc.
    private val databaseProvider: suspend () -> MyDatabase = { AppDatabaseProvider.get() },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ReleaseReminderRepository {
    override fun observeHasReminder(key: ReminderKey): Flow<Boolean> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectReleaseReminder(key.mediaId, key.mediaType, key.seasonColumn(), key.episodeColumn())
                    .asFlow()
                    .mapToOneOrNull(Dispatchers.Default)
                    .map { it != null },
            )
        }

    override fun observeReminderKeys(): Flow<Set<ReminderKey>> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectReleaseReminderKeys()
                    .asFlow()
                    .mapToList(Dispatchers.Default)
                    .map { rows ->
                        rows
                            .map { reminderKey(it.mediaId, it.mediaType, it.seasonNumber, it.episodeNumber) }
                            .toSet()
                    },
            )
        }

    override suspend fun setReminder(
        reminder: ReleaseReminder,
        enabled: Boolean,
    ) {
        val queries = databaseProvider().myDatabaseQueries
        val key = reminder.key
        if (enabled) {
            queries.insertReleaseReminder(
                mediaId = key.mediaId,
                mediaType = key.mediaType,
                seasonNumber = key.seasonColumn(),
                episodeNumber = key.episodeColumn(),
                title = reminder.title,
                posterPath = reminder.posterPath,
                releaseDate = reminder.releaseDate.toString(),
                releaseSource = reminder.releaseSource,
                addedAt = now(),
                lastCheckedAt = null,
            )
        } else {
            queries.deleteReleaseReminder(key.mediaId, key.mediaType, key.seasonColumn(), key.episodeColumn())
        }
    }

    override suspend fun allReminders(): List<ReleaseReminder> =
        databaseProvider()
            .myDatabaseQueries
            .selectAllReleaseReminders()
            .awaitAsList()
            .map { it.toReleaseReminder() }

    override suspend fun updateReleaseDate(
        key: ReminderKey,
        releaseDate: LocalDate,
        releaseSource: String?,
    ) {
        databaseProvider().myDatabaseQueries.updateReleaseReminderDate(
            releaseDate = releaseDate.toString(),
            releaseSource = releaseSource,
            lastCheckedAt = now(),
            mediaId = key.mediaId,
            mediaType = key.mediaType,
            seasonNumber = key.seasonColumn(),
            episodeNumber = key.episodeColumn(),
        )
    }

    override suspend fun deleteExpired(today: LocalDate) {
        val cutoff = today.minus(DatePeriod(days = ReleaseReminderConstant.EXPIRY_DAYS))
        databaseProvider().myDatabaseQueries.deleteReleaseRemindersBefore(cutoff.toString())
    }

    override suspend fun deleteAll() {
        databaseProvider().myDatabaseQueries.deleteAllReleaseReminders()
    }

    override fun observePreference(): Flow<ReminderPreference> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectReminderPreference()
                    .asFlow()
                    .mapToOneOrNull(Dispatchers.Default)
                    .map { it.toReminderPreference() },
            )
        }

    override suspend fun preference(): ReminderPreference =
        databaseProvider()
            .myDatabaseQueries
            .selectReminderPreference()
            .awaitAsOneOrNull()
            .toReminderPreference()

    override suspend fun setPreference(preference: ReminderPreference) {
        databaseProvider().myDatabaseQueries.upsertReminderPreference(
            enabled = if (preference.enabled) 1L else 0L,
            minutesOfDay = preference.time.toMinutesOfDay().toLong(),
        )
    }
}

private fun ReminderKey.seasonColumn(): Long = seasonNumber?.toLong() ?: ReleaseReminderConstant.NO_EPISODE

private fun ReminderKey.episodeColumn(): Long = episodeNumber?.toLong() ?: ReleaseReminderConstant.NO_EPISODE

private fun reminderKey(
    mediaId: Long,
    mediaType: String,
    seasonNumber: Long,
    episodeNumber: Long,
): ReminderKey =
    ReminderKey(
        mediaId = mediaId,
        mediaType = mediaType,
        seasonNumber = seasonNumber.takeIf { it != ReleaseReminderConstant.NO_EPISODE }?.toInt(),
        episodeNumber = episodeNumber.takeIf { it != ReleaseReminderConstant.NO_EPISODE }?.toInt(),
    )

private fun ReleaseReminderRow.toReleaseReminder(): ReleaseReminder =
    ReleaseReminder(
        key = reminderKey(mediaId, mediaType, seasonNumber, episodeNumber),
        title = title,
        posterPath = posterPath,
        releaseDate = LocalDate.parse(releaseDate),
        releaseSource = releaseSource,
    )

private fun ReminderPreferenceRow?.toReminderPreference(): ReminderPreference =
    if (this == null) {
        ReminderPreference(enabled = ReleaseReminderConstant.DEFAULT_ENABLED, time = ReleaseReminderConstant.DEFAULT_TIME)
    } else {
        ReminderPreference(enabled = enabled != 0L, time = minutesOfDay.toInt().toLocalTime())
    }

internal fun LocalTime.toMinutesOfDay(): Int = hour * ReleaseReminderConstant.MINUTES_PER_HOUR + minute

internal fun Int.toLocalTime(): LocalTime =
    LocalTime(this / ReleaseReminderConstant.MINUTES_PER_HOUR, this % ReleaseReminderConstant.MINUTES_PER_HOUR)
