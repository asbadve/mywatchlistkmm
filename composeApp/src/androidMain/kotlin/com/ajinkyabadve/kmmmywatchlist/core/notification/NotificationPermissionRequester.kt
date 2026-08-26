@file:JvmName("NotificationPermissionRequesterAndroid")

package com.ajinkyabadve.kmmmywatchlist.core.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private class AndroidNotificationPermissionRequester(
    private val alreadyGranted: () -> Boolean,
    private val launch: (CancellableContinuation<Boolean>) -> Unit,
) : NotificationPermissionRequester {
    override suspend fun request(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || alreadyGranted()) return true
        return suspendCancellableCoroutine { continuation -> launch(continuation) }
    }
}

@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester {
    val context = LocalContext.current
    var pendingContinuation by remember { mutableStateOf<CancellableContinuation<Boolean>?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingContinuation?.resume(granted)
            pendingContinuation = null
        }
    return remember(context, launcher) {
        AndroidNotificationPermissionRequester(
            alreadyGranted = {
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            },
            launch = { continuation ->
                pendingContinuation = continuation
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
        )
    }
}
