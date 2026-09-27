package com.ajinkyabadve.kmmmywatchlist.core.notification

/**
 * Schedules/cancels the periodic background job that runs `TvEpisodeNotificationPoller.poll()` -
 * see `shipped_features.md` item 3's shared-infrastructure checklist for the per-platform
 * mechanism (`WorkManager`/`BGTaskScheduler`/a JVM scheduled executor/`setInterval`). Not
 * `@Composable`, same reasoning as `LocalNotifier`.
 */
expect object NotificationScheduler {
    fun schedule()

    fun cancel()
}
