package com.ajinkyabadve.kmmmywatchlist.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ajinkyabadve.kmmmywatchlist.AndroidApp
import com.ajinkyabadve.kmmmywatchlist.features.account.usecase.SendQueuedTogglesUseCase
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private object AndroidQueuedToggleSyncConstant {
    const val WORK_NAME = "queued_toggle_sync"

    // Past this, stop retrying until the next app start or offline toggle schedules a fresh job -
    // a rejected session (401) keeps entries queued, and retrying that forever helps nobody.
    const val MAX_ATTEMPTS = 5
    val INITIAL_BACKOFF = 30.seconds
}

/** Sends the queue once the network is connected; retries with backoff while changes remain. */
class QueuedToggleSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        when {
            SendQueuedTogglesUseCase()() -> Result.success()
            runAttemptCount + 1 >= AndroidQueuedToggleSyncConstant.MAX_ATTEMPTS -> Result.success()
            else -> Result.retry()
        }
}

actual object QueuedToggleSyncScheduler {
    actual fun schedule() {
        val request =
            OneTimeWorkRequestBuilder<QueuedToggleSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, AndroidQueuedToggleSyncConstant.INITIAL_BACKOFF.toJavaDuration())
                .build()
        // APPEND_OR_REPLACE, not KEEP: a toggle queued while a send is already running must get a
        // run of its own after it - KEEP would drop it if the running send had already read the queue.
        WorkManager
            .getInstance(AndroidApp.instance)
            .enqueueUniqueWork(AndroidQueuedToggleSyncConstant.WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
