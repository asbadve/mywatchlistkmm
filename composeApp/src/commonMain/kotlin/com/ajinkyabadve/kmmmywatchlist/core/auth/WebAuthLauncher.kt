package com.ajinkyabadve.kmmmywatchlist.core.auth

import androidx.compose.runtime.Composable

interface WebAuthLauncher {
    fun launchAuth(
        authUrl: String,
        redirectScheme: String = "mywatchlist",
        onResult: (requestToken: String?, approved: Boolean) -> Unit,
    )

    fun checkPendingAuth(onResult: (requestToken: String, approved: Boolean) -> Unit) {}
}

@Composable
expect fun rememberWebAuthLauncher(): WebAuthLauncher
