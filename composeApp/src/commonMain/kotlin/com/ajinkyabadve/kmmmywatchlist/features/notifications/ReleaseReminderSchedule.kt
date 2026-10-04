package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderKey
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

internal object ReleaseReminderScheduleConstant {
    /** Only reminders firing within this many days are handed to the OS. iOS keeps at most 64
     *  pending requests per app (silently dropping the rest); two per title leaves room for ~32
     *  titles plus item 3's notifications. The poller tops up later ones as they come into range. */
    const val HORIZON_DAYS = 30
    const val ID_PREFIX = "release"
    const val ID_SEPARATOR = ":"
}

/** The two reminders each release gets. [idSuffix] is part of the OS request id, so it must
 *  never change once shipped (a change would orphan already-scheduled requests). */
enum class ReminderKind(
    val idSuffix: String,
) {
    DAY_BEFORE("day_before"),
    RELEASE_DAY("release_day"),
}

/**
 * When the two reminders for a release on [releaseDate] should fire: the day before and the day
 * itself, both at [time] (device-local). Anything at or before [now] is dropped, so a reminder
 * added late never sends a stale "out tomorrow", and anything past [horizonDays] from [now] is
 * dropped to stay under iOS's pending-request cap.
 */
fun reminderFireTimes(
    releaseDate: LocalDate,
    time: LocalTime,
    now: LocalDateTime,
    horizonDays: Int = ReleaseReminderScheduleConstant.HORIZON_DAYS,
): List<Pair<ReminderKind, LocalDateTime>> {
    val horizon = now.date.plus(DatePeriod(days = horizonDays)).atTime(now.time)
    return listOf(
        ReminderKind.DAY_BEFORE to releaseDate.minus(DatePeriod(days = 1)).atTime(time),
        ReminderKind.RELEASE_DAY to releaseDate.atTime(time),
    ).filter { (_, fireAt) -> fireAt > now && fireAt <= horizon }
}

/** The stable OS request id for one reminder - the same key and kind always map to the same id,
 *  so rescheduling replaces a request instead of duplicating it. */
fun ReminderKey.requestId(kind: ReminderKind): String =
    listOf(
        ReleaseReminderScheduleConstant.ID_PREFIX,
        mediaType,
        mediaId,
        seasonNumber ?: NO_EPISODE_ID_PART,
        episodeNumber ?: NO_EPISODE_ID_PART,
        kind.idSuffix,
    ).joinToString(ReleaseReminderScheduleConstant.ID_SEPARATOR)

/** Both request ids for [this] key - what to cancel when a reminder is removed or rescheduled. */
fun ReminderKey.allRequestIds(): List<String> = ReminderKind.entries.map { requestId(it) }

private const val NO_EPISODE_ID_PART = "-"
