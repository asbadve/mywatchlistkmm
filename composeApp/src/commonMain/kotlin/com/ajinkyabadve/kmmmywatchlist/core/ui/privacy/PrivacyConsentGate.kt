package com.ajinkyabadve.kmmmywatchlist.core.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.core.constant.PrivacyConsentConstant
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.PrivacyConsentRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.PrivacyConsentRepositoryImpl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.privacy_consent_accept_button
import mywatchlist.composeapp.generated.resources.privacy_consent_checkbox_label
import mywatchlist.composeapp.generated.resources.privacy_consent_title
import org.jetbrains.compose.resources.stringResource

object PrivacyConsentGateConstant {
    const val CHECKBOX_TAG = "privacyConsentCheckbox"
    const val ACCEPT_BUTTON_TAG = "privacyConsentAcceptButton"
}

/**
 * Blocks [content] behind a one-time privacy-policy acceptance screen - required by TMDB's API
 * terms and the app stores alike before the app touches any account or local data. Shown once per
 * install: [PrivacyConsentRepository] persists acceptance, so a later launch skips straight to
 * [content]. There is deliberately no way to see [content] without accepting - no back-press
 * dismissal, no skip action, no dialog-style "outside tap to close".
 */
@Composable
fun PrivacyConsentGate(
    privacyConsentRepository: PrivacyConsentRepository = remember { PrivacyConsentRepositoryImpl() },
    content: @Composable () -> Unit,
) {
    var accepted by remember { mutableStateOf(privacyConsentRepository.hasAcceptedPrivacyPolicy()) }
    if (accepted) {
        content()
    } else {
        PrivacyConsentScreen(
            onAccept = {
                privacyConsentRepository.markPrivacyPolicyAccepted()
                accepted = true
            },
        )
    }
}

@Composable
private fun PrivacyConsentScreen(onAccept: () -> Unit) {
    var checked by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .toggleable(
                                    value = checked,
                                    role = Role.Checkbox,
                                    onValueChange = { checked = it },
                                ).testTag(PrivacyConsentGateConstant.CHECKBOX_TAG),
                    ) {
                        // Click handling lives on the Row (toggleable above), not here - tapping
                        // the label text is the standard way users expect to toggle a checkbox,
                        // and `onCheckedChange = null` avoids a second, redundant click target.
                        Checkbox(checked = checked, onCheckedChange = null)
                        Text(stringResource(Res.string.privacy_consent_checkbox_label))
                    }
                    Button(
                        onClick = onAccept,
                        enabled = checked,
                        modifier = Modifier.fillMaxWidth().testTag(PrivacyConsentGateConstant.ACCEPT_BUTTON_TAG),
                    ) {
                        Text(stringResource(Res.string.privacy_consent_accept_button))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Text(
                text = stringResource(Res.string.privacy_consent_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
            )
            PrivacyPolicyContent(
                url = PrivacyConsentConstant.PRIVACY_POLICY_URL,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
    }
}
