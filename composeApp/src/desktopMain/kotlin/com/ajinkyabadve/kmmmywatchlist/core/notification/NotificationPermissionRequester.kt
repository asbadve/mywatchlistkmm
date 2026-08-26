@file:JvmName("NotificationPermissionRequesterDesktop")

package com.ajinkyabadve.kmmmywatchlist.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// The JVM has no OS-level notification permission to request - SystemTray.displayMessage works
// (or silently doesn't, if unsupported) with no prompt, so this always reports granted.
private object DesktopNotificationPermissionRequester : NotificationPermissionRequester {
    override suspend fun request(): Boolean = true
}

@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester = remember { DesktopNotificationPermissionRequester }
