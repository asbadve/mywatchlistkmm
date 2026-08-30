package com.ajinkyabadve.kmmmywatchlist.features.notifications.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase

/**
 * The closed set of reasons `TvEpisodeNotificationPoller` can notify for - see its kdoc for what
 * triggers each. [storageValue] is what's actually written to `notificationLedger.reason` (a
 * `TEXT` column, see `MyDatabase.sq`): kept as the pre-existing "episode_announced"/"episode_airing"
 * strings rather than switching to `name` so already-recorded rows keep matching after this type
 * changed from a bare `String` to this enum (code-conventions §9).
 */
enum class NotificationReason(
    val storageValue: String,
) {
    EPISODE_ANNOUNCED("episode_announced"),
    EPISODE_AIRING("episode_airing"),

    // future_features_checklist.md item 3b: a favorited person's combined_credits gained a
    // credit id it didn't have on the previous poll - see PersonCreditNotificationPoller.
    PERSON_NEW_CREDIT("person_new_credit"),

    // future_features_checklist.md item 3c: a favorited collection's parts gained a movie id it
    // didn't have on the previous poll - see CollectionNotificationPoller.
    COLLECTION_NEW_PART("collection_new_part"),
}

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
        reason: NotificationReason,
        cursorValue: String,
    ): Boolean

    /** Records that a notification for this exact (id, mediaType, reason, cursorValue) was just sent. */
    suspend fun recordNotified(
        id: Int,
        mediaType: String,
        reason: NotificationReason,
        cursorValue: String,
        notifiedAt: Long,
    )

    /** Debug-only: wipes every dedup row for one [reason] - scoped rather than the whole table so
     *  AccountScreen's separate "Poll episode notifications now" and "Poll person notifications
     *  now" debug rows never clobber each other's dedup state. */
    suspend fun clearForReasonForDebug(reason: NotificationReason)
}

class NotificationLedgerRepositoryImpl(
    // Same test seam as TrackedMediaRepositoryImpl's databaseProvider - see its kdoc.
    private val databaseProvider: suspend () -> MyDatabase = { AppDatabaseProvider.get() },
) : NotificationLedgerRepository {
    override suspend fun alreadyNotified(
        id: Int,
        mediaType: String,
        reason: NotificationReason,
        cursorValue: String,
    ): Boolean {
        val row =
            databaseProvider()
                .myDatabaseQueries
                .selectNotificationLedgerRow(id.toLong(), mediaType, reason.storageValue)
                .awaitAsOneOrNull()
        return row?.cursorValue == cursorValue
    }

    override suspend fun recordNotified(
        id: Int,
        mediaType: String,
        reason: NotificationReason,
        cursorValue: String,
        notifiedAt: Long,
    ) {
        databaseProvider().myDatabaseQueries.upsertNotificationLedgerRow(
            id = id.toLong(),
            mediaType = mediaType,
            reason = reason.storageValue,
            cursorValue = cursorValue,
            notifiedAt = notifiedAt,
        )
    }

    override suspend fun clearForReasonForDebug(reason: NotificationReason) {
        databaseProvider().myDatabaseQueries.clearNotificationLedgerForDebugByReason(reason.storageValue)
    }
}
