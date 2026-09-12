package com.ajinkyabadve.kmmmywatchlist.core.ui.privacy

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.privacy_consent_load_error_message
import mywatchlist.composeapp.generated.resources.privacy_consent_retry_button
import org.jetbrains.compose.resources.stringResource

/**
 * `javaScriptEnabled` (off by [WebView.getSettings]'s default, unlike iOS's `WKWebView` which
 * defaults to on) - the hosted page's own back-to-top button needs it, and a Claude Artifact URL
 * was tried first for [com.ajinkyabadve.kmmmywatchlist.core.constant.PrivacyConsentConstant.PRIVACY_POLICY_URL]
 * that genuinely required scripting just to render at all (its JS-driven viewer chrome loads real
 * content through a cross-origin frame handshake that doesn't complete inside an embedded WebView -
 * confirmed 2026-09-12 on an emulator: main-frame navigation "succeeded" from WebView's perspective
 * while the page stayed on its own loading spinner forever). Now a plain static page hosted on
 * GitHub Pages instead, but there's no reason to turn scripting back off. `SuppressLint` because
 * this WebView only ever loads that one hardcoded URL, never arbitrary/user-supplied content, so
 * the usual XSS concern `javaScriptEnabled` triggers a lint warning for doesn't apply here.
 *
 * A plain [WebView] with no [WebViewClient] also never reports failures back to the app and would
 * leave its own built-in loading spinner stuck forever on any real network failure - unacceptable
 * for a gate the user cannot otherwise get past. This tracks load failures and offers a retry that
 * reloads the same [WebView] instance directly, rather than depending on Compose recomposition to
 * drive [AndroidView]'s `update`.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun PrivacyPolicyContent(
    url: String,
    modifier: Modifier,
) {
    var hasError by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient =
                        object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView,
                                url: String?,
                                favicon: Bitmap?,
                            ) {
                                hasError = false
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                if (request.isForMainFrame) hasError = true
                            }
                        }
                    loadUrl(url)
                    webView = this
                }
            },
        )
        if (hasError) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(Res.string.privacy_consent_load_error_message),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                Button(onClick = { webView?.reload() }) {
                    Text(stringResource(Res.string.privacy_consent_retry_button))
                }
            }
        }
    }
}
