package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabaseQueries
import com.ajinkyabadve.kmmmywatchlist.db.TrackedMediaOutbox
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException

/**
 * Sends the offline favorite/watchlist toggles queued in `trackedMediaOutbox` (see its comment in
 * `MyDatabase.sq`) to TMDB. [TrackedMediaRepository.refreshAll] calls [flush] first, so a queued
 * change reaches TMDB before that refresh prunes. [TrackedMediaRemoteMediator]'s REFRESH does not
 * (offline, waiting on this send's retries kept the grid blank twice as long); its prune keeps queued
 * rows instead, until the next refreshAll sends them.
 *
 * Per entry, oldest first:
 * - **Sent** - the entry goes; a sent removal also hard-deletes its hidden row. A malformed
 *   response body counts as sent: the request reached TMDB.
 * - **Still offline** ([IOException]) - stop and keep everything; the rest would fail the same way.
 * - **Session rejected** (401) - stop and keep everything, so it is retried after signing in again.
 * - **Any other rejection** - drop the entry and fall back to what TMDB has: a rejected removal's
 *   row is shown again; a rejected addition is left to the following refresh to prune.
 *
 * No platform equivalent: WorkManager would only cover Android, and the queue has to survive on
 * iOS and desktop too, so it lives in the shared SQLite database.
 */
internal class TrackedMediaOutboxFlusher(
    private val accountMediaRepository: AccountMediaRepository,
    private val queries: MyDatabaseQueries,
) {
    /**
     * Returns true when nothing is left queued for [accountId], false when entries are still
     * waiting. One send at a time app-wide ([SEND_LOCK]): the app-start send and a refresh can
     * overlap, and two concurrent sends would read the same rows and send each twice.
     */
    suspend fun flush(
        accountId: Long,
        sessionId: String,
    ): Boolean =
        SEND_LOCK.withLock {
            for (entry in queries.selectOutboxEntriesForAccount(accountId).awaitAsList()) {
                if (send(entry, accountId, sessionId) == SendResult.STOP) return@withLock false
            }
            true
        }

    private suspend fun send(
        entry: TrackedMediaOutbox,
        accountId: Long,
        sessionId: String,
    ): SendResult {
        val category = AccountMediaCategory.entries.firstOrNull { it.storageValue == entry.category }
        if (category == null) {
            // Unreachable unless the stored category values change - nothing could ever send it.
            Napier.e(tag = TAG) { UNKNOWN_CATEGORY_MESSAGE }
            queries.deleteOutboxEntry(entry.id, entry.mediaType, entry.category, accountId)
            return SendResult.CONTINUE
        }
        val desiredState = entry.desiredState == TrackedMediaOutboxConstant.DESIRED_ADD
        return try {
            when (category) {
                AccountMediaCategory.FAVORITES ->
                    accountMediaRepository.setFavorite(accountId, sessionId, entry.mediaType, entry.id, desiredState)
                AccountMediaCategory.WATCHLIST ->
                    accountMediaRepository.setWatchlist(accountId, sessionId, entry.mediaType, entry.id, desiredState)
            }
            onSent(entry, accountId, desiredState)
        } catch (e: IOException) {
            Napier.e(tag = TAG, throwable = e) { STILL_OFFLINE_MESSAGE }
            SendResult.STOP
        } catch (e: HttpExceptions) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                Napier.e(tag = TAG, throwable = e) { SESSION_REJECTED_MESSAGE }
                SendResult.STOP
            } else {
                Napier.e(tag = TAG, throwable = e) { REJECTED_MESSAGE }
                onRejected(entry, accountId, desiredState)
            }
        } catch (e: ContentConvertException) {
            Napier.e(tag = TAG, throwable = e) { MALFORMED_RESPONSE_MESSAGE }
            onSent(entry, accountId, desiredState)
        } catch (e: SerializationException) {
            Napier.e(tag = TAG, throwable = e) { MALFORMED_RESPONSE_MESSAGE }
            onSent(entry, accountId, desiredState)
        }
    }

    private suspend fun onSent(
        entry: TrackedMediaOutbox,
        accountId: Long,
        desiredState: Boolean,
    ): SendResult {
        queries.transaction {
            queries.deleteOutboxEntry(entry.id, entry.mediaType, entry.category, accountId)
            if (!desiredState) queries.deleteTrackedMediaRow(entry.id, entry.mediaType, entry.category)
        }
        return SendResult.CONTINUE
    }

    private suspend fun onRejected(
        entry: TrackedMediaOutbox,
        accountId: Long,
        desiredState: Boolean,
    ): SendResult {
        queries.transaction {
            queries.deleteOutboxEntry(entry.id, entry.mediaType, entry.category, accountId)
            if (!desiredState) queries.clearTrackedMediaPendingDelete(entry.id, entry.mediaType, entry.category)
        }
        return SendResult.CONTINUE
    }

    private enum class SendResult { CONTINUE, STOP }

    private companion object {
        val SEND_LOCK = Mutex()
        const val TAG = "TrackedMediaOutbox"
        const val STILL_OFFLINE_MESSAGE = "Still offline - keeping the queued toggles"
        const val SESSION_REJECTED_MESSAGE = "Session rejected - keeping the queued toggles until the next sign-in"
        const val REJECTED_MESSAGE = "TMDB rejected a queued toggle - dropping it and keeping TMDB's state"
        const val MALFORMED_RESPONSE_MESSAGE = "Malformed response to a queued toggle - treating it as sent"
        const val UNKNOWN_CATEGORY_MESSAGE = "Queued toggle has an unknown category - dropping it"
    }
}
