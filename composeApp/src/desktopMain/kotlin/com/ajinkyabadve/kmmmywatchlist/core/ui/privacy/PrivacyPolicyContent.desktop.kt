package com.ajinkyabadve.kmmmywatchlist.core.ui.privacy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun PrivacyPolicyContent(
    url: String,
    modifier: Modifier,
) {
    PrivacyPolicySummaryContent(url, modifier)
}
