@file:JvmName("NotificationSchedulerDesktop")

package com.ajinkyabadve.kmmmywatchlist.core.notification

import com.ajinkyabadve.kmmmywatchlist.features.notifications.CollectionNotificationPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.PersonCreditNotificationPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.TvEpisodeNotificationPoller
import io.github.aakira.napier.Napier
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private object DesktopNotificationSchedulerConstant {
    const val TAG = "NotificationSchedulerDesktop"
    const val POLL_INTERVAL_HOURS = 6L
}

// Best-effort only, unlike Android/iOS's OS-level background scheduling - polling only happens
// while the JVM process is actually running, and stops entirely when the app is closed.
actual object NotificationScheduler {
    private val executor =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "episode-notification-poll").apply {
                isDaemon =
                    true
            }
        }
    private var scheduledFuture: ScheduledFuture<*>? = null

    actual fun schedule() {
        scheduledFuture?.cancel(false)
        scheduledFuture =
            executor.scheduleWithFixedDelay(
                {
                    runCatching {
                        runBlocking {
                            TvEpisodeNotificationPoller().poll()
                            PersonCreditNotificationPoller().poll()
                            CollectionNotificationPoller().poll()
                        }
                    }.onFailure { Napier.e(tag = DesktopNotificationSchedulerConstant.TAG, throwable = it) { "Poll cycle failed" } }
                },
                0,
                DesktopNotificationSchedulerConstant.POLL_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
    }

    actual fun cancel() {
        scheduledFuture?.cancel(false)
        scheduledFuture = null
    }
}
