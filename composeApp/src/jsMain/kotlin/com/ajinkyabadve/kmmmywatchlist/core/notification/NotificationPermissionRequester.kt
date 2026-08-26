package com.ajinkyabadve.kmmmywatchlist.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.await
import org.w3c.notifications.GRANTED
import org.w3c.notifications.Notification
import org.w3c.notifications.NotificationPermission

private class JsNotificationPermissionRequester : NotificationPermissionRequester {
    override suspend fun request(): Boolean {
        if (Notification.permission == NotificationPermission.GRANTED) return true
        return Notification.requestPermission().await() == NotificationPermission.GRANTED
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester = remember { JsNotificationPermissionRequester() }
