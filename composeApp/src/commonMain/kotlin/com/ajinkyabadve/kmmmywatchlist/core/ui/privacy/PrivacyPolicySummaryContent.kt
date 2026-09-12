package com.ajinkyabadve.kmmmywatchlist.core.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.openUrl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.privacy_consent_intro
import mywatchlist.composeapp.generated.resources.privacy_consent_open_full_policy
import mywatchlist.composeapp.generated.resources.privacy_consent_point_account
import mywatchlist.composeapp.generated.resources.privacy_consent_point_local
import mywatchlist.composeapp.generated.resources.privacy_consent_point_no_tracking
import mywatchlist.composeapp.generated.resources.privacy_consent_point_notifications
import mywatchlist.composeapp.generated.resources.tmdb_attribution_notice
import org.jetbrains.compose.resources.stringResource

/**
 * Desktop/JS's [PrivacyPolicyContent] - see that expect fun's kdoc for why these two platforms get
 * a native summary instead of an embedded browser. Covers the same points as the full hosted
 * policy at [url] in brief, with a link to open the full text in the system browser.
 */
@Composable
internal fun PrivacyPolicySummaryContent(
    url: String,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.privacy_consent_intro), style = MaterialTheme.typography.bodyMedium)
        BulletPoint(stringResource(Res.string.privacy_consent_point_account))
        BulletPoint(stringResource(Res.string.privacy_consent_point_local))
        BulletPoint(stringResource(Res.string.privacy_consent_point_notifications))
        BulletPoint(stringResource(Res.string.privacy_consent_point_no_tracking))
        Text(
            text = stringResource(Res.string.tmdb_attribution_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { openUrl(url) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.privacy_consent_open_full_policy))
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Text(
        text = "•  $text",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Normal,
    )
}
