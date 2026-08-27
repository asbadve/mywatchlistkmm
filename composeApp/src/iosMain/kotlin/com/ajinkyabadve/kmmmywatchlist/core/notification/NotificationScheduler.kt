package com.ajinkyabadve.kmmmywatchlist.core.notification

import com.ajinkyabadve.kmmmywatchlist.features.notifications.PersonCreditNotificationPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.TvEpisodeNotificationPoller
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGAppRefreshTask
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow

private object IosNotificationSchedulerConstant {
    // Must also be declared under BGTaskSchedulerPermittedIdentifiers in Info.plist (Xcode's
    // synthesized-plist build settings do not reliably support this custom array key - add it via
    // Xcode's Info tab if background polling isn't firing).
    const val TASK_IDENTIFIER = "com.ajinkyabadve.kmmmywatchlist.episodePoll"
    const val POLL_INTERVAL_SECONDS = 6.0 * 60.0 * 60.0
}

@OptIn(ExperimentalForeignApi::class)
actual object NotificationScheduler {
    init {
        BGTaskScheduler.sharedScheduler().registerForTaskWithIdentifier(
            identifier = IosNotificationSchedulerConstant.TASK_IDENTIFIER,
            usingQueue = null,
        ) { task ->
            handleTask(task as BGAppRefreshTask)
        }
    }

    // Called once, unconditionally, from Main.kt's MainViewController() at app launch. Apple
    // requires registerForTaskWithIdentifier to run before the app finishes launching - crashes
    // with NSInternalInconsistencyException ("All launch handlers must be registered before
    // application finishes launching") otherwise. Kotlin objects are lazy singletons - this
    // object's init block above only ran the first time something touched NotificationScheduler,
    // which (before this) was only when a user toggled the setting on, long after launch. This
    // function's body does nothing; it exists purely to force that touch to happen at launch.
    fun ensureRegisteredAtLaunch() {}

    private fun handleTask(task: BGAppRefreshTask) {
        scheduleNextRequest()
        val job =
            CoroutineScope(Dispatchers.Default).launch {
                TvEpisodeNotificationPoller().poll()
                PersonCreditNotificationPoller().poll()
                task.setTaskCompletedWithSuccess(true)
            }
        task.expirationHandler = { job.cancel() }
    }

    actual fun schedule() {
        scheduleNextRequest()
    }

    private fun scheduleNextRequest() {
        val request = BGAppRefreshTaskRequest(IosNotificationSchedulerConstant.TASK_IDENTIFIER)
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(IosNotificationSchedulerConstant.POLL_INTERVAL_SECONDS)
        BGTaskScheduler.sharedScheduler().submitTaskRequest(request, null)
    }

    actual fun cancel() {
        BGTaskScheduler.sharedScheduler().cancelTaskRequestWithIdentifier(IosNotificationSchedulerConstant.TASK_IDENTIFIER)
    }
}
