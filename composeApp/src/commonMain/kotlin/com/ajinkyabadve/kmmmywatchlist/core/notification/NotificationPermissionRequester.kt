package com.ajinkyabadve.kmmmywatchlist.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Requests whatever OS-level permission [LocalNotifier.post] needs - separate from [LocalNotifier]
 * itself because requesting (unlike posting) needs an Activity/composition context on Android, the
 * same reason `core/auth/WebAuthLauncher.kt` is its own `@Composable expect fun remember...()`
 * rather than a plain object.
 */
interface NotificationPermissionRequester {
    /** True once permission is granted (already-granted counts as success, no re-prompt). */
    suspend fun request(): Boolean
}

/**
 * Overrides the platform requester for everything below it - `null` (the default, and all the app
 * itself ever uses) means [rememberPlatformNotificationPermissionRequester]. UI tests provide a
 * fake here: on iOS the real requester calls `UNUserNotificationCenter`, which aborts a test
 * binary that isn't an app bundle, and a fake also lets a test choose granted or denied.
 * A CompositionLocal rather than a parameter because six composables ask for a requester.
 */
val LocalNotificationPermissionRequester: ProvidableCompositionLocal<NotificationPermissionRequester?> =
    staticCompositionLocalOf { null }

@Composable
fun rememberNotificationPermissionRequester(): NotificationPermissionRequester =
    LocalNotificationPermissionRequester.current ?: rememberPlatformNotificationPermissionRequester()

@Composable
expect fun rememberPlatformNotificationPermissionRequester(): NotificationPermissionRequester
