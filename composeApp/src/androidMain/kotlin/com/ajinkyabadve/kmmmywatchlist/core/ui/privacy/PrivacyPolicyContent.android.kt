package com.ajinkyabadve.kmmmywatchlist.core.ui.privacy

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal actual fun PrivacyPolicyContent(
    url: String,
    modifier: Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context -> WebView(context) },
        update = { webView -> webView.loadUrl(url) },
    )
}
