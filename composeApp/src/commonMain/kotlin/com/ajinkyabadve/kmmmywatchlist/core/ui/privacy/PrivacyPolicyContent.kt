package com.ajinkyabadve.kmmmywatchlist.core.ui.privacy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders the privacy policy at [url] inside [PrivacyConsentGate]. A real embedded browser on
 * Android/iOS (native `WebView`/`WKWebView` - both free on-platform, no new dependency). Compose
 * Desktop has no built-in WebView; embedding one there (JCEF) would add real native-binary weight
 * to every desktop installer for a screen shown exactly once per install, so Desktop/JS instead
 * render the policy natively in Compose with a link that opens [url] in the system browser (see
 * the `release-process` skill for this trade-off).
 */
@Composable
internal expect fun PrivacyPolicyContent(
    url: String,
    modifier: Modifier,
)
