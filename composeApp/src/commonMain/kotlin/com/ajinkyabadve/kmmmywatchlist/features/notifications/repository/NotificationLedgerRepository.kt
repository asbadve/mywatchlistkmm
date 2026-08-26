package com.ajinkyabadve.kmmmywatchlist.features.notifications.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase

/**
 * Local SQLite is the source of truth for "has a notification already been sent for this exact
 * (media, reason, cursor value)" - see `MyDatabase.sq`'s `notificationLedger` table kdoc for why
 * `cursorValue` (not just id/mediaType/reason) is part of the dedup key: it's what lets the same
 * reason fire again for a different episode air date (a later season) while still suppressing an
 * exact repeat for the same one.
 */
interface NotificationLedgerRepository {
    /** True if a notification for this exact (id, mediaType, reason, cursorValue) already went out. */
    suspend fun alreadyNotified(
        id: Int,
        mediaType: String,
        reason: String,
        cursorValue: String,
    ): Boolean

    /** Records that a notification for this exact (id, mediaType, reason, cursorValue) was just sent. */
    suspend fun recordNotified(
        id: Int,
        mediaType: String,
        reason: String,
        cursorValue: String,
        notifiedAt: Long,
    )

    /** Debug-only: wipes every dedup row - see AccountScreen's "Poll episode notifications now" row. */
    suspend fun clearAllForDebug()
}

class NotificationLedgerRepositoryImpl(
    // Same test seam as TrackedMediaRepositoryImpl's databaseProvider - see its kdoc.
    private val databaseProvider: suspend () -> MyDatabase = { AppDatabaseProvider.get() },
) : NotificationLedgerRepository {
    override suspend fun alreadyNotified(
        id: Int,
        mediaType: String,
        reason: String,
        cursorValue: String,
    ): Boolean {
        val row =
            databaseProvider()
                .myDatabaseQueries
                .selectNotificationLedgerRow(id.toLong(), mediaType, reason)
                .awaitAsOneOrNull()
        return row?.cursorValue == cursorValue
    }

    override suspend fun recordNotified(
        id: Int,
        mediaType: String,
        reason: String,
        cursorValue: String,
        notifiedAt: Long,
    ) {
        databaseProvider().myDatabaseQueries.upsertNotificationLedgerRow(
            id = id.toLong(),
            mediaType = mediaType,
            reason = reason,
            cursorValue = cursorValue,
            notifiedAt = notifiedAt,
        )
    }

    override suspend fun clearAllForDebug() {
        databaseProvider().myDatabaseQueries.clearNotificationLedgerForDebug()
    }
}
