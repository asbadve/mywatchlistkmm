@file:JvmName("LocalNotifierDesktop")

package com.ajinkyabadve.kmmmywatchlist.core.notification

import io.github.aakira.napier.Napier
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

private object DesktopNotificationConstant {
    const val TAG = "LocalNotifierDesktop"
}

// JVM has no persistent notification tray entry (unlike Android/iOS) - a TrayIcon is only a
// carrier for TrayIcon.displayMessage, so this must add/keep one icon around for the process
// lifetime rather than creating a new one per notification.
private val trayIcon: TrayIcon? by lazy {
    if (!SystemTray.isSupported()) return@lazy null
    val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val icon = TrayIcon(image, "MyWatchList")
    icon.isImageAutoSize = true
    // TrayIcon has no per-message click callback (unlike Android's PendingIntent or iOS's
    // userInfo) - only one action listener for the icon as a whole, fired on the balloon click or
    // an icon double-click. Approximated here as "open whatever the most recently posted
    // notification was about", which is correct for the common case of one live notification at a
    // time and only wrong if several stack up before the user clicks.
    icon.addActionListener { lastPostedDeepLink?.let { PendingEpisodeNotificationTarget.set(it) } }
    runCatching { SystemTray.getSystemTray().add(icon) }
        .onFailure { Napier.e(tag = DesktopNotificationConstant.TAG, throwable = it) { "Failed to add tray icon" } }
    icon
}

private var lastPostedDeepLink: EpisodeNotificationTarget? = null

actual object LocalNotifier {
    // posterUrl unused: java.awt.TrayIcon.displayMessage has no image parameter at all - AWT/Swing
    // tray balloons are text-only, unlike Android's BigPictureStyle or the browser Notification
    // API's icon/image options.
    actual suspend fun post(
        notificationId: Int,
        title: String,
        body: String,
        deepLink: EpisodeNotificationTarget,
        posterUrl: String?,
    ) {
        lastPostedDeepLink = deepLink
        trayIcon?.displayMessage(title, body, TrayIcon.MessageType.INFO)
    }
}
