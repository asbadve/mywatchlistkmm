@file:JvmName("NotificationSchedulerAndroid")

package com.ajinkyabadve.kmmmywatchlist.core.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ajinkyabadve.kmmmywatchlist.AndroidApp
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
        return Result.success()
    }
}

actual object NotificationScheduler {
    actual fun schedule() {
        val request =
            PeriodicWorkRequestBuilder<EpisodeNotificationWorker>(AndroidNotificationSchedulerConstant.POLL_INTERVAL.toJavaDuration())
                .build()
        WorkManager
            .getInstance(AndroidApp.instance)
            .enqueueUniquePeriodicWork(AndroidNotificationSchedulerConstant.WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    actual fun cancel() {
        WorkManager.getInstance(AndroidApp.instance).cancelUniqueWork(AndroidNotificationSchedulerConstant.WORK_NAME)
    }
}
