package com.ajinkyabadve.kmmmywatchlist.core.ui.privacy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView

@Composable
internal actual fun PrivacyPolicyContent(
    url: String,
    modifier: Modifier,
) {
    UIKitView(
        factory = { WKWebView() },
        modifier = modifier,
        update = { webView ->
            NSURL.URLWithString(url)?.let { nsUrl ->
                webView.loadRequest(NSURLRequest(nsUrl))
            }
        },
    )
}
