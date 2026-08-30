@file:JvmName("NotificationSchedulerAndroid")

package com.ajinkyabadve.kmmmywatchlist.core.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ajinkyabadve.kmmmywatchlist.AndroidApp
import com.ajinkyabadve.kmmmywatchlist.features.notifications.CollectionNotificationPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.PersonCreditNotificationPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.TvEpisodeNotificationPoller
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

private object AndroidNotificationSchedulerConstant {
    const val WORK_NAME = "episode_notification_poll"

    // TMDB's data doesn't change fast enough to justify WorkManager's 15-minute OS floor - a
    // longer interval trades timeliness for battery, deliberately.
    val POLL_INTERVAL = 6.hours
}

class EpisodeNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        TvEpisodeNotificationPoller().poll()
        // Same "episode_notifications_enabled" schedule covers 3b/3c too - one periodic job, not a
        // second/third one, since it's the same underlying permission/preference (see
        // PersonCreditNotificationPoller's kdoc).
        PersonCreditNotificationPoller().poll()
        CollectionNotificationPoller().poll()
        return Result.success()
    }
}

actual object NotificationScheduler {
    actual fun schedule() {
        // Every poller call hits TMDB - without this, WorkManager can run the job with no
        // connectivity, doWork() silently swallows every HttpExceptions/IOException per item (see
        // each poller's per-item try/catch) and still returns Result.success(), so the job never
        // gets retried even though it polled nothing. This makes WorkManager wait for connectivity
        // instead.
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request =
            PeriodicWorkRequestBuilder<EpisodeNotificationWorker>(AndroidNotificationSchedulerConstant.POLL_INTERVAL.toJavaDuration())
                .setConstraints(constraints)
                .build()
        WorkManager
            .getInstance(AndroidApp.instance)
            .enqueueUniquePeriodicWork(AndroidNotificationSchedulerConstant.WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    actual fun cancel() {
        WorkManager.getInstance(AndroidApp.instance).cancelUniqueWork(AndroidNotificationSchedulerConstant.WORK_NAME)
    }
}
