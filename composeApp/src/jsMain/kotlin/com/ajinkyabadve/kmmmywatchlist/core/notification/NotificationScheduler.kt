package com.ajinkyabadve.kmmmywatchlist.core.notification

import com.ajinkyabadve.kmmmywatchlist.features.notifications.CollectionNotificationPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.PersonCreditNotificationPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.TvEpisodeNotificationPoller
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private object JsNotificationSchedulerConstant {
    // Best-effort only, unlike Android/iOS's OS-level background scheduling - polling only
    // happens while this browser tab is open, and stops the moment it's closed.
    const val POLL_INTERVAL_MILLIS = 6 * 60 * 60 * 1000
}

actual object NotificationScheduler {
    private var intervalId: Int? = null

    actual fun schedule() {
        intervalId?.let { window.clearInterval(it) }
        intervalId =
            window.setInterval({
                CoroutineScope(Dispatchers.Default).launch {
                    TvEpisodeNotificationPoller().poll()
                    PersonCreditNotificationPoller().poll()
                    CollectionNotificationPoller().poll()
                }
            }, JsNotificationSchedulerConstant.POLL_INTERVAL_MILLIS)
    }

    actual fun cancel() {
        intervalId?.let { window.clearInterval(it) }
        intervalId = null
    }
}
