package com.ajinkyabadve.kmmmywatchlist.core.constant

object PrivacyConsentConstant {
    const val KEY_PRIVACY_POLICY_ACCEPTED = "privacy_policy_accepted_v1"

    // Plain static HTML on GitHub Pages (gh-pages branch), not a Claude Artifact URL - confirmed
    // 2026-09-12 that an artifact page's cross-origin frame-shell architecture doesn't render
    // inside an embedded WebView/WKWebView (only a full top-level browser tab), so it silently
    // never loaded in PrivacyConsentGate despite working fine when opened in a real browser.
    const val PRIVACY_POLICY_URL = "https://asbadve.github.io/mywatchlistkmm/privacy-policy.html"
}
