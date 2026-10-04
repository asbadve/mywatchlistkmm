package com.ajinkyabadve.kmmmywatchlist.core.notification

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSDateComponents
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

private object IosReminderSchedulerConstant {
    /** Every reminder id starts with this (see `ReleaseReminderSchedule.requestId`), which is how
     *  the debug listing tells reminders apart from item 3's notifications. */
    const val REMINDER_ID_PREFIX = "release:"
    const val MILLIS_PER_SECOND = 1000.0
}

actual fun platformReminderScheduler(): ReminderScheduler = IosReminderScheduler

/**
 * iOS's [ReminderScheduler]: a `UNNotificationRequest` with a non-repeating
 * `UNCalendarNotificationTrigger`, which the OS delivers on time with the app closed and keeps
 * across reboots. iOS holds at most 64 pending requests per app and silently drops the rest - the
 * caller's 30-day horizon (`ReleaseReminderScheduleConstant.HORIZON_DAYS`) keeps it under that.
 * The content is built by [LocalNotifier.buildContent], so a reminder looks and taps exactly like
 * every other notification this app posts.
 */
internal object IosReminderScheduler : ReminderScheduler {
    override suspend fun schedule(
        id: String,
        at: LocalDateTime,
        content: ReminderContent,
        windowMinutes: Int,
    ) {
        val components =
            NSDateComponents().apply {
                year = at.year.toLong()
                month = at.monthNumber.toLong()
                day = at.dayOfMonth.toLong()
                hour = at.hour.toLong()
                minute = at.minute.toLong()
            }
        val request =
            UNNotificationRequest.requestWithIdentifier(
                identifier = id,
                content = LocalNotifier.buildContent(content.title, content.body, content.target, content.posterUrl),
                trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false),
            )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
    }

    override suspend fun cancel(ids: Collection<String>) {
        if (ids.isEmpty()) return
        UNUserNotificationCenter.currentNotificationCenter().removePendingNotificationRequestsWithIdentifiers(ids.toList())
    }

    override suspend fun pendingForDebug(expected: Map<String, LocalDateTime>): List<PendingReminder> =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().getPendingNotificationRequestsWithCompletionHandler { requests ->
                val pending =
                    requests
                        .orEmpty()
                        .filterIsInstance<UNNotificationRequest>()
                        .filter { it.identifier.startsWith(IosReminderSchedulerConstant.REMINDER_ID_PREFIX) }
                        .map { request ->
                            val nextFire = (request.trigger as? UNCalendarNotificationTrigger)?.nextTriggerDate()
                            PendingReminder(
                                id = request.identifier,
                                fireAt =
                                    nextFire?.let { date ->
                                        Instant
                                            .fromEpochMilliseconds(
                                                (
                                                    date.timeIntervalSince1970 *
                                                        IosReminderSchedulerConstant.MILLIS_PER_SECOND
                                                ).toLong(),
                                            ).toLocalDateTime(TimeZone.currentSystemDefault())
                                    },
                                isRegistered = true,
                            )
                        }.sortedBy { it.fireAt }
                continuation.resume(pending)
            }
        }
}
