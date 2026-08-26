package com.ajinkyabadve.kmmmywatchlist.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
private class IosNotificationPermissionRequester : NotificationPermissionRequester {
    override suspend fun request(): Boolean =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter
                .currentNotificationCenter()
                .requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
                ) { granted, _ ->
                    continuation.resume(granted)
                }
        }
}

@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester = remember { IosNotificationPermissionRequester() }
