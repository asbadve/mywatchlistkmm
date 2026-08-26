package com.ajinkyabadve.kmmmywatchlist.core.notification

import androidx.compose.runtime.Composable

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

@Composable
expect fun rememberNotificationPermissionRequester(): NotificationPermissionRequester
