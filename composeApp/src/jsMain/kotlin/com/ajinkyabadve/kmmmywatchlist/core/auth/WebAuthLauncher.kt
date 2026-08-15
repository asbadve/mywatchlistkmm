package com.ajinkyabadve.kmmmywatchlist.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

class JsWebAuthLauncher : WebAuthLauncher {
    override fun launchAuth(
        authUrl: String,
        redirectScheme: String,
        onResult: (requestToken: String?, approved: Boolean) -> Unit,
    ) {
        val currentOrigin = window.location.origin + window.location.pathname
        val fullAuthUrl =
            if (authUrl.contains("?")) {
                "$authUrl&redirect_to=$currentOrigin"
            } else {
                "$authUrl?redirect_to=$currentOrigin"
            }
        window.location.href = fullAuthUrl
    }

    override fun checkPendingAuth(onResult: (requestToken: String, approved: Boolean) -> Unit) {
        val search = window.location.search
        if (search.contains("request_token=")) {
            val params =
                search.removePrefix("?").split("&").associate {
                    val parts = it.split("=")
                    val key = parts.getOrNull(0).orEmpty()
                    val value = parts.getOrNull(1).orEmpty()
                    key to value
                }
            val requestToken = params["request_token"]
            val approved = params["approved"] != "false"
            if (!requestToken.isNullOrEmpty()) {
                // Strip query parameters from the browser URL bar
                window.history.replaceState(null, "", window.location.pathname)
                onResult(requestToken, approved)
            }
        }
    }
}

@Composable
actual fun rememberWebAuthLauncher(): WebAuthLauncher = remember { JsWebAuthLauncher() }
