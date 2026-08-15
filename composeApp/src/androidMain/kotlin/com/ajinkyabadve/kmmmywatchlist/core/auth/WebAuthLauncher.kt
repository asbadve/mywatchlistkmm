@file:JvmName("WebAuthLauncherAndroid")

package com.ajinkyabadve.kmmmywatchlist.core.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.aakira.napier.Napier

object AndroidAuthCallbackHandler {
    private var pendingCallback: ((requestToken: String?, approved: Boolean) -> Unit)? = null

    fun setCallback(callback: (requestToken: String?, approved: Boolean) -> Unit) {
        pendingCallback = callback
    }

    fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "mywatchlist") {
            val requestToken = data.getQueryParameter("request_token")
            val approved = data.getBooleanQueryParameter("approved", true) || data.getQueryParameter("approved") == "true"
            Napier.d(tag = "AndroidAuthCallback") { "Deep link received: token=$requestToken, approved=$approved" }
            pendingCallback?.invoke(requestToken, approved)
            pendingCallback = null
        }
    }
}

class AndroidWebAuthLauncher(
    private val context: Context,
) : WebAuthLauncher {
    override fun launchAuth(
        authUrl: String,
        redirectScheme: String,
        onResult: (requestToken: String?, approved: Boolean) -> Unit,
    ) {
        AndroidAuthCallbackHandler.setCallback(onResult)
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(context, Uri.parse(authUrl))
        } catch (e: android.content.ActivityNotFoundException) {
            Napier.e(tag = "WebAuthLauncherAndroid", throwable = e) { "No browser application found to handle Custom Tabs" }
            onResult(null, false)
        } catch (e: SecurityException) {
            Napier.e(tag = "WebAuthLauncherAndroid", throwable = e) { "Security exception launching Custom Tabs" }
            onResult(null, false)
        } catch (e: IllegalArgumentException) {
            Napier.e(tag = "WebAuthLauncherAndroid", throwable = e) { "Invalid auth URL passed to Custom Tabs" }
            onResult(null, false)
        }
    }
}

@Composable
actual fun rememberWebAuthLauncher(): WebAuthLauncher {
    val context = LocalContext.current
    return remember(context) { AndroidWebAuthLauncher(context) }
}
