package com.ajinkyabadve.kmmmywatchlist.core.notification

import kotlinx.datetime.LocalDateTime

object ReminderSchedulerConstant {
    /** Android delivery window for a real reminder - "around 9:00" rather than exactly, which is
     *  all a date-only release needs and needs no exact-alarm permission. */
    const val DEFAULT_WINDOW_MINUTES = 30
}

/** What a scheduled reminder shows when it fires - the same fields [LocalNotifier.post] takes,
 *  handed to the OS ahead of time instead of posted now. */
data class ReminderContent(
    val notificationId: Int,
    val title: String,
    val body: String,
    val posterUrl: String?,
    val target: NotificationTarget,
)

/** One reminder the OS currently holds, for the debug "Show pending reminders" row. [isRegistered]
 *  is whether the platform confirms it is actually scheduled (Android checks the alarm's
 *  PendingIntent still exists; iOS only ever lists requests it holds, so it's always true there). */
data class PendingReminder(
    val id: String,
    val fireAt: LocalDateTime?,
    val isRegistered: Boolean,
)

/**
 * Hands a local notification to the OS to deliver at a future local date-time, even with the app
 * closed - checklist item 16's release reminders. An interface (not an `expect object` like
 * [LocalNotifier]) so the coordinator and poller can be unit-tested against a fake.
 *
 * Implementations: iOS uses `UNCalendarNotificationTrigger` (delivered on time by the OS, max 64
 * pending requests per app); Android uses an inexact `AlarmManager.setWindow` alarm whose receiver
 * posts through [LocalNotifier] (alarms are wiped on reboot, so a boot receiver reschedules);
 * desktop and JS have no OS scheduler that fires with the app closed, so theirs is a no-op.
 *
 * Platform equivalents considered and not used:
 * - The Alarmee KMP library wraps exactly these two APIs, but this app already owns notification
 *   content, posters, permission and tap-to-open in [LocalNotifier] - a library would duplicate it.
 * - A WorkManager one-time request survives reboot by itself, but Doze can defer it by hours.
 * - Exact alarms: `USE_EXACT_ALARM` is a Play-restricted permission for alarm/calendar apps, and
 *   `SCHEDULE_EXACT_ALARM` is denied by default on Android 14+. A release date has no time of day,
 *   so a ~30-minute window is enough.
 */
interface ReminderScheduler {
    /**
     * Schedules (or replaces, if [id] is already scheduled) a reminder for [at], device-local time.
     * [windowMinutes] is how late Android may deliver it (an inexact alarm's window; Android 12+
     * clips anything under 10 minutes up to 10). iOS delivers on time and ignores it.
     */
    suspend fun schedule(
        id: String,
        at: LocalDateTime,
        content: ReminderContent,
        windowMinutes: Int = ReminderSchedulerConstant.DEFAULT_WINDOW_MINUTES,
    )

    /** Cancels any of [ids] that are scheduled; unknown ids are ignored. Never cancels anything
     *  else, so item 3's notifications are untouched. */
    suspend fun cancel(ids: Collection<String>)

    /** What's currently scheduled, for the debug row. [expected] maps the ids the database says
     *  should exist to their fire times - Android has no API to list alarms, so it checks these
     *  one by one; iOS lists what it actually holds. */
    suspend fun pendingForDebug(expected: Map<String, LocalDateTime>): List<PendingReminder>
}

/** The real scheduler for the current platform. */
expect fun platformReminderScheduler(): ReminderScheduler
