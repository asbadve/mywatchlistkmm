package com.ajinkyabadve.kmmmywatchlist.core.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** What a tapped local notification should navigate to - carried through the platform
 *  notification itself (Android's `PendingIntent` extras, iOS's `UNNotificationContent.userInfo`)
 *  so it survives a cold launch, not just a tap while already running. Closed set, per
 *  code-conventions §9 - every notification this app posts is about exactly one of these. */
sealed interface NotificationTarget

/** Which episode a [TvEpisodeNotificationPoller][com.ajinkyabadve.kmmmywatchlist.features.notifications.TvEpisodeNotificationPoller]
 *  notification was about. */
data class EpisodeNotificationTarget(
    val tvShowId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
) : NotificationTarget

/** Which person a [PersonCreditNotificationPoller][com.ajinkyabadve.kmmmywatchlist.features.notifications.PersonCreditNotificationPoller]
 *  new-credit notification was about - Android and iOS only (see [LocalNotifier.post]'s kdoc for
 *  why Desktop/JS deliberately don't act on this variant, the same way Desktop already doesn't for
 *  [EpisodeNotificationTarget]'s TrayIcon click limitation). */
data class PersonNotificationTarget(
    val personId: Long,
) : NotificationTarget

/**
 * Set by a platform's notification-tap handler (Android's `AppActivity`, iOS's
 * `UNUserNotificationCenterDelegate`, JS's `Notification.onclick`, Desktop's `TrayIcon`
 * `ActionListener`) - possibly before `App()`'s composition even exists yet, so this is a plain
 * observable holder rather than a one-shot callback. `App.kt`'s `MainAppScreen` observes [current]
 * and pushes the matching nav keys once per genuinely new target (see its `LaunchedEffect`).
 */
object PendingNotificationTarget {
    var current: NotificationTarget? by mutableStateOf(null)
        private set

    fun set(target: NotificationTarget) {
        current = target
    }

    fun consume() {
        current = null
    }
}
