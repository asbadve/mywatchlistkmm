package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.notification.MediaDetailNotificationTarget
import com.ajinkyabadve.kmmmywatchlist.core.notification.PendingReminder
import com.ajinkyabadve.kmmmywatchlist.core.notification.ReminderContent
import com.ajinkyabadve.kmmmywatchlist.core.notification.ReminderScheduler
import com.ajinkyabadve.kmmmywatchlist.core.notification.platformReminderScheduler
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderPreference
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.debug_test_reminder_body
import mywatchlist.composeapp.generated.resources.debug_test_reminder_title
import mywatchlist.composeapp.generated.resources.notification_episode_today_title
import mywatchlist.composeapp.generated.resources.notification_episode_tomorrow_title
import mywatchlist.composeapp.generated.resources.notification_release_body_digital
import mywatchlist.composeapp.generated.resources.notification_release_body_episode
import mywatchlist.composeapp.generated.resources.notification_release_body_premiere
import mywatchlist.composeapp.generated.resources.notification_release_body_primary
import mywatchlist.composeapp.generated.resources.notification_release_body_theatrical
import mywatchlist.composeapp.generated.resources.notification_release_today_title
import mywatchlist.composeapp.generated.resources.notification_release_tomorrow_title
import org.jetbrains.compose.resources.getString

internal object ReleaseReminderCoordinatorConstant {
    /** The debug "Fire test reminder" row's request id - outside the `release:<type>:...` shape so
     *  it can never collide with a real reminder. */
    const val DEBUG_TEST_REQUEST_ID = "release:debug:test"
    const val DEBUG_TEST_DELAY_MINUTES = 1L

    /** Shortest window Android 12+ honours for `setWindow` - the debug reminder asks for the
     *  tightest delivery a no-permission alarm can get. */
    const val DEBUG_TEST_WINDOW_MINUTES = 10

    /** Used by the debug reminder when no real reminder exists yet (Fight Club - any stable,
     *  always-present TMDB movie id works). */
    const val DEBUG_SAMPLE_MOVIE_ID = 550L

    const val POSTER_TARGET_WIDTH_DP = 500
    const val POSTER_DENSITY = 1f
}

/**
 * Turns the release reminders stored in the database into OS-scheduled notifications. Every
 * operation is idempotent - it cancels a reminder's request ids before scheduling them again - so
 * it is safe to call [rescheduleAll] from anywhere the schedule might have drifted: app start,
 * Android boot, a changed reminder time, the end of every [ReleaseReminderPoller] run.
 */
class ReleaseReminderCoordinator(
    private val repository: ReleaseReminderRepository = ReleaseReminderRepositoryImpl(),
    private val scheduler: ReminderScheduler = platformReminderScheduler(),
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val now: () -> LocalDateTime = { Clock.System.now().toLocalDateTime(timeZone()) },
) {
    suspend fun rescheduleAll() {
        val preference = repository.preference()
        repository.allReminders().forEach { reminder -> reschedule(reminder, preference) }
    }

    /** Adds or removes one reminder and updates its OS requests to match. */
    suspend fun setReminder(
        reminder: ReleaseReminder,
        enabled: Boolean,
    ) {
        repository.setReminder(reminder, enabled)
        if (enabled) {
            reschedule(reminder, repository.preference())
        } else {
            scheduler.cancel(reminder.key.allRequestIds())
        }
    }

    /** Saves a new preference and moves every pending reminder to match it. */
    suspend fun setPreference(preference: ReminderPreference) {
        repository.setPreference(preference)
        rescheduleAll()
    }

    /** Debug: cancels every scheduled reminder (and the test one) and deletes all reminder rows. */
    suspend fun clearAll() {
        val ids = repository.allReminders().flatMap { it.key.allRequestIds() } + ReleaseReminderCoordinatorConstant.DEBUG_TEST_REQUEST_ID
        scheduler.cancel(ids)
        repository.deleteAll()
    }

    /** Debug: what the OS currently holds, checked against what the database says should exist. */
    suspend fun pendingForDebug(): List<PendingReminder> {
        val preference = repository.preference()
        val expected =
            if (!preference.enabled) {
                emptyMap()
            } else {
                repository
                    .allReminders()
                    .flatMap { reminder ->
                        reminderFireTimes(reminder.releaseDate, preference.time, now()).map { (kind, fireAt) ->
                            reminder.key.requestId(kind) to fireAt
                        }
                    }.toMap()
            }
        return scheduler.pendingForDebug(expected)
    }

    /**
     * Debug: schedules a real OS reminder [ReleaseReminderCoordinatorConstant.DEBUG_TEST_DELAY_MINUTES]
     * out, to check delivery with the app closed, after a reboot, and tap-to-open. Uses the first
     * stored reminder's content when there is one. Returns when it should fire.
     */
    suspend fun scheduleTestReminder(): LocalDateTime {
        val zone = timeZone()
        val fireAt =
            Clock.System
                .now()
                .plus(ReleaseReminderCoordinatorConstant.DEBUG_TEST_DELAY_MINUTES, DateTimeUnit.MINUTE, zone)
                .toLocalDateTime(zone)
        val reminder = repository.allReminders().firstOrNull()
        val content =
            if (reminder != null) {
                buildContent(reminder, ReminderKind.RELEASE_DAY, ReleaseReminderCoordinatorConstant.DEBUG_TEST_REQUEST_ID)
            } else {
                ReminderContent(
                    notificationId = ReleaseReminderCoordinatorConstant.DEBUG_TEST_REQUEST_ID.hashCode(),
                    title = getString(Res.string.debug_test_reminder_title),
                    body = getString(Res.string.debug_test_reminder_body),
                    posterUrl = null,
                    target =
                        MediaDetailNotificationTarget(
                            ReleaseReminderCoordinatorConstant.DEBUG_SAMPLE_MOVIE_ID,
                            MediaTypeConstant.MOVIE,
                        ),
                )
            }
        scheduler.schedule(
            id = ReleaseReminderCoordinatorConstant.DEBUG_TEST_REQUEST_ID,
            at = fireAt,
            content = content,
            windowMinutes = ReleaseReminderCoordinatorConstant.DEBUG_TEST_WINDOW_MINUTES,
        )
        return fireAt
    }

    private suspend fun reschedule(
        reminder: ReleaseReminder,
        preference: ReminderPreference,
    ) {
        scheduler.cancel(reminder.key.allRequestIds())
        if (!preference.enabled) return
        reminderFireTimes(reminder.releaseDate, preference.time, now()).forEach { (kind, fireAt) ->
            val id = reminder.key.requestId(kind)
            scheduler.schedule(id = id, at = fireAt, content = buildContent(reminder, kind, id))
        }
    }

    private suspend fun buildContent(
        reminder: ReleaseReminder,
        kind: ReminderKind,
        requestId: String,
    ): ReminderContent {
        val key = reminder.key
        val seasonNumber = key.seasonNumber
        val episodeNumber = key.episodeNumber
        val isEpisode = seasonNumber != null && episodeNumber != null
        val title =
            when {
                isEpisode && kind == ReminderKind.DAY_BEFORE -> getString(Res.string.notification_episode_tomorrow_title, reminder.title)
                isEpisode -> getString(Res.string.notification_episode_today_title, reminder.title)
                kind == ReminderKind.DAY_BEFORE -> getString(Res.string.notification_release_tomorrow_title, reminder.title)
                else -> getString(Res.string.notification_release_today_title, reminder.title)
            }
        val source = ReleaseSource.decode(reminder.releaseSource)
        val region = source.regionCode
        val body =
            when {
                seasonNumber != null && episodeNumber != null ->
                    getString(Res.string.notification_release_body_episode, seasonNumber, episodeNumber)
                source.kind == ReleaseSourceKind.THEATRICAL && region != null ->
                    getString(Res.string.notification_release_body_theatrical, region)
                source.kind == ReleaseSourceKind.DIGITAL && region != null ->
                    getString(Res.string.notification_release_body_digital, region)
                source.kind == ReleaseSourceKind.FIRST_AIR -> getString(Res.string.notification_release_body_premiere)
                else -> getString(Res.string.notification_release_body_primary)
            }
        val posterUrl =
            ImageConfigResolver.resolve(
                reminder.posterPath,
                ImageConfigResolver.ImageType.POSTER,
                ReleaseReminderCoordinatorConstant.POSTER_TARGET_WIDTH_DP,
                ReleaseReminderCoordinatorConstant.POSTER_DENSITY,
            )
        return ReminderContent(
            notificationId = requestId.hashCode(),
            title = title,
            body = body,
            posterUrl = posterUrl,
            target = MediaDetailNotificationTarget(key.mediaId, key.mediaType),
        )
    }
}

/** The instant a device-local [LocalDateTime] refers to, in [timeZone]. */
internal fun LocalDateTime.toEpochMillis(timeZone: TimeZone): Long = toInstant(timeZone).toEpochMilliseconds()
