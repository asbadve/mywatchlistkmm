package com.ajinkyabadve.kmmmywatchlist.core.ui.privacy

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
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ObjCSignatureOverride
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.privacy_consent_load_error_message
import mywatchlist.composeapp.generated.resources.privacy_consent_retry_button
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

/**
 * A [WKWebView] with no navigation delegate never reports failures back to the app - unacceptable
 * for a gate the user cannot otherwise get past. See the Android actual's kdoc for the concrete
 * failure this guards against (confirmed on an emulator, not iOS-specific, but the same class of
 * risk applies here). Tracks load failures via [WKNavigationDelegateProtocol] and offers a retry
 * that reloads the same [WKWebView] instance directly.
 */
@Composable
internal actual fun PrivacyPolicyContent(
    url: String,
    modifier: Modifier,
) {
    var hasError by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WKWebView?>(null) }
    val navigationDelegate =
        remember {
            object : NSObject(), WKNavigationDelegateProtocol {
                override fun webView(
                    webView: WKWebView,
                    didStartProvisionalNavigation: WKNavigation?,
                ) {
                    hasError = false
                }

                @ObjCSignatureOverride
                override fun webView(
                    webView: WKWebView,
                    didFailProvisionalNavigation: WKNavigation?,
                    withError: NSError,
                ) {
                    hasError = true
                }

                @ObjCSignatureOverride
                override fun webView(
                    webView: WKWebView,
                    didFailNavigation: WKNavigation?,
                    withError: NSError,
                ) {
                    hasError = true
                }
            }
        }

    Box(modifier = modifier) {
        UIKitView(
            factory = {
                WKWebView().apply {
                    this.navigationDelegate = navigationDelegate
                    NSURL.URLWithString(url)?.let { nsUrl -> loadRequest(NSURLRequest(nsUrl)) }
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
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
