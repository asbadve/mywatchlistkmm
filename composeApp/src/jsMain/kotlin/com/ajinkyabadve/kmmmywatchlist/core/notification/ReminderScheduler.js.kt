package com.ajinkyabadve.kmmmywatchlist.core.notification

import kotlinx.datetime.LocalDateTime

actual fun platformReminderScheduler(): ReminderScheduler = NoOpReminderScheduler

/** This platform has no OS scheduler that fires with the app closed, so release reminders aren't
 *  delivered here at all - the reminder UI is hidden by `isMobilePlatform()` rather than promising
 *  something that mostly wouldn't fire. */
internal object NoOpReminderScheduler : ReminderScheduler {
    override suspend fun schedule(
        id: String,
        at: LocalDateTime,
        content: ReminderContent,
        windowMinutes: Int,
    ) = Unit

    override suspend fun cancel(ids: Collection<String>) = Unit

    override suspend fun pendingForDebug(expected: Map<String, LocalDateTime>): List<PendingReminder> = emptyList()
}
