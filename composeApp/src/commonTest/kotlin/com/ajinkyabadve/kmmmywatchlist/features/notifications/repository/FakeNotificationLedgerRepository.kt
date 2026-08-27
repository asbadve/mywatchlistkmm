package com.ajinkyabadve.kmmmywatchlist.features.notifications.repository

/** In-memory stand-in for [NotificationLedgerRepository] - deliberately dumb, same reasoning as
 *  every other Fake*Repository in this codebase (see e.g. FakeTrackedMediaRepository's kdoc). */
class FakeNotificationLedgerRepository : NotificationLedgerRepository {
    private data class Key(
        val id: Int,
        val mediaType: String,
        val reason: NotificationReason,
    )

    private val cursors = mutableMapOf<Key, String>()
    val recordNotifiedCalls = mutableListOf<Quadruple>()

    data class Quadruple(
        val id: Int,
        val mediaType: String,
        val reason: NotificationReason,
        val cursorValue: String,
    )

    override suspend fun alreadyNotified(
        id: Int,
        mediaType: String,
        reason: NotificationReason,
        cursorValue: String,
    ): Boolean = cursors[Key(id, mediaType, reason)] == cursorValue

    override suspend fun recordNotified(
        id: Int,
        mediaType: String,
        reason: NotificationReason,
        cursorValue: String,
        notifiedAt: Long,
    ) {
        cursors[Key(id, mediaType, reason)] = cursorValue
        recordNotifiedCalls.add(Quadruple(id, mediaType, reason, cursorValue))
    }

    override suspend fun clearForReasonForDebug(reason: NotificationReason) {
        cursors.keys.filter { it.reason == reason }.forEach { cursors.remove(it) }
    }
}
