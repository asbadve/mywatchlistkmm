package com.ajinkyabadve.kmmmywatchlist.core.notification

import kotlinx.datetime.LocalDateTime

/** Records what would be handed to the OS; [scheduled] mirrors what the OS would currently hold. */
class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableMapOf<String, LocalDateTime>()
    val scheduledContent = mutableMapOf<String, ReminderContent>()
    val scheduleCalls = mutableListOf<String>()
    val cancelledIds = mutableListOf<String>()
    val windowMinutesById = mutableMapOf<String, Int>()

    override suspend fun schedule(
        id: String,
        at: LocalDateTime,
        content: ReminderContent,
        windowMinutes: Int,
    ) {
        scheduleCalls.add(id)
        scheduled[id] = at
        scheduledContent[id] = content
        windowMinutesById[id] = windowMinutes
    }

    override suspend fun cancel(ids: Collection<String>) {
        cancelledIds.addAll(ids)
        ids.forEach { id ->
            scheduled.remove(id)
            scheduledContent.remove(id)
        }
    }

    override suspend fun pendingForDebug(expected: Map<String, LocalDateTime>): List<PendingReminder> =
        expected.map { (id, fireAt) -> PendingReminder(id = id, fireAt = fireAt, isRegistered = id in scheduled) }
}
