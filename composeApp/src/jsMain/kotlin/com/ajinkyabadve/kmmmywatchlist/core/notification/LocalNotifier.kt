package com.ajinkyabadve.kmmmywatchlist.core.notification

import kotlinx.browser.window
import org.w3c.notifications.GRANTED
import org.w3c.notifications.Notification
import org.w3c.notifications.NotificationOptions
import org.w3c.notifications.NotificationPermission

actual object LocalNotifier {
    actual suspend fun post(
        notificationId: Int,
        title: String,
        body: String,
        deepLink: NotificationTarget?,
        posterUrl: String?,
    ) {
        if (Notification.permission != NotificationPermission.GRANTED) return
        // Unlike Android/iOS, the browser fetches the image itself from the URL - no download/decode
        // needed here, just pass it straight through.
        val notification =
            Notification(
                title,
                NotificationOptions(body = body, tag = notificationId.toString(), icon = posterUrl, image = posterUrl),
            )
        // Unlike Android's PendingIntent/iOS's userInfo, this only works while the tab that posted
        // it is still open - the app is already running in both cases, so there's no separate
        // "cold launch from notification" path to handle here, just bring the tab to front and let
        // PendingNotificationTarget's existing observer (App.kt) do the rest. Only ever acts on an
        // EpisodeNotificationTarget - PersonNotificationTarget (3b) is deliberately not wired up on
        // JS (confirmed 2026-08-26); either way the tab still focuses on click.
        notification.onclick = {
            window.focus()
            (deepLink as? EpisodeNotificationTarget)?.let { PendingNotificationTarget.set(it) }
        }
    }
}
