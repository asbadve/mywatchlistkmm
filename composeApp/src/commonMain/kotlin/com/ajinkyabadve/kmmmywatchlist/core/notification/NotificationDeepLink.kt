package com.ajinkyabadve.kmmmywatchlist.core.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Which episode a [TvEpisodeNotificationPoller][com.ajinkyabadve.kmmmywatchlist.features.notifications.TvEpisodeNotificationPoller]
 *  notification was about - carried through the platform notification (Android's `PendingIntent`
 *  extras, iOS's `UNNotificationContent.userInfo`) so a tap can navigate straight to it. */
data class EpisodeNotificationTarget(
    val tvShowId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
)

/**
 * Set by a platform's notification-tap handler (Android's `AppActivity`, iOS's
 * `UNUserNotificationCenterDelegate`, JS's `Notification.onclick`, Desktop's `TrayIcon`
 * `ActionListener`) - possibly before `App()`'s composition even exists yet, so this is a plain
 * observable holder rather than a one-shot callback. `App.kt`'s `MainAppScreen` observes [current]
 * and pushes the matching nav keys once per genuinely new target (see its `LaunchedEffect`).
 */
object PendingEpisodeNotificationTarget {
    var current: EpisodeNotificationTarget? by mutableStateOf(null)
        private set

    fun set(target: EpisodeNotificationTarget) {
        current = target
    }

    fun consume() {
        current = null
    }
}
