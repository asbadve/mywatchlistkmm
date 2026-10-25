package com.ajinkyabadve.kmmmywatchlist.core.notification

/**
 * What the pollers post through - [LocalNotifier] in the app. An interface because [LocalNotifier]
 * is an `expect object` and can't be faked: tests pass a fake instead, which also matters on iOS,
 * where the real one calls `UNUserNotificationCenter` and aborts a test binary that isn't an app
 * bundle. See [LocalNotifier.post] for the parameters.
 */
interface Notifier {
    suspend fun post(
        notificationId: Int,
        title: String,
        body: String,
        deepLink: NotificationTarget?,
        posterUrl: String?,
    )
}
