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
            // Fail closed: TMDB only appends approved=true on a genuine approval redirect, and per
            // its docs doesn't redirect at all on denial. Treating a missing/unexpected value as
            // approved would let an unapproved token reach createSession(), which TMDB rejects with
            // 401 - surfacing as a confusing "expired" error instead of the correct "cancelled" one.
            val approved = data.getQueryParameter("approved") == "true"
            Napier.d(tag = "AndroidAuthCallback") { "Deep link received: token=$requestToken, approved=$approved" }
            pendingCallback?.invoke(requestToken, approved)
            pendingCallback = null
        }
    }

    // The Custom Tab redirects back via handleIntent() on approval, which runs before onResume()
    // and clears pendingCallback. If the user instead denies/backs out of the TMDB page, no deep
    // link ever arrives - only onResume() fires - so a callback still pending here means the user
    // returned without completing auth. Treat that as a cancellation instead of leaving the caller
    // (and its spinner) waiting forever.
    fun handleResume() {
        val callback = pendingCallback ?: return
        Napier.d(tag = "AndroidAuthCallback") { "Resumed with no deep link - treating as user cancelled" }
        pendingCallback = null
        callback.invoke(null, false)
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
