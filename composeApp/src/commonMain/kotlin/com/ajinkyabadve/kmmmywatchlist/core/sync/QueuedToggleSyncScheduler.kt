package com.ajinkyabadve.kmmmywatchlist.core.sync

import com.ajinkyabadve.kmmmywatchlist.features.account.usecase.SendQueuedTogglesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Gets offline favorite/watchlist changes to TMDB without the user opening My Fav. Called at app
 * start and right after a change is queued (`MediaActionsState`).
 * - Android: a one-off WorkManager job that waits for a network connection and retries with
 *   backoff - so a change made offline is sent when the connection returns, even with the app
 *   closed. This is the pattern Android's offline-first guidance recommends
 *   (developer.android.com/topic/architecture/data-layer/offline-first).
 * - iOS and desktop: one send now, in the background ([sendQueuedTogglesNow]). iOS background
 *   tasks can't wait on "network connected", and desktop has no equivalent; My Fav's refresh
 *   covers the rest.
 * - Web: nothing - it never queues (see `NetworkOnlyTrackedMediaRepositoryImpl`).
 */
expect object QueuedToggleSyncScheduler {
    fun schedule()
}

/** App-lifetime scope for [sendQueuedTogglesNow]: a send must outlive the screen that queued it. */
private object QueuedToggleSyncScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

/** The iOS/desktop [QueuedToggleSyncScheduler.schedule] body - one send, now, off the main thread. */
internal fun sendQueuedTogglesNow(sendQueuedToggles: SendQueuedTogglesUseCase = SendQueuedTogglesUseCase()) {
    QueuedToggleSyncScope.scope.launch { sendQueuedToggles() }
}
