package com.ajinkyabadve.kmmmywatchlist.core.ui.privacy

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakePrivacyConsentRepository
import kotlin.test.Test
import kotlin.test.assertEquals

private const val GATED_CONTENT_TEXT = "Gated content"

// The platform renderers embed a native web view the test harness can't host (on iOS it throws),
// so every test renders the shared Compose summary instead.
private val summaryPolicyContent: @Composable (String, Modifier) -> Unit =
    { url, modifier -> PrivacyPolicySummaryContent(url, modifier) }

@OptIn(ExperimentalTestApi::class)
class PrivacyConsentGateUiTest {
    @Test
    fun testAlreadyAccepted_rendersContentImmediately() =
        runComposeUiTest {
            setContent {
                PrivacyConsentGate(
                    privacyConsentRepository = FakePrivacyConsentRepository(accepted = true),
                    policyContent = summaryPolicyContent,
                ) {
                    Text(GATED_CONTENT_TEXT)
                }
            }

            onNodeWithText(GATED_CONTENT_TEXT).assertExists()
            onNodeWithTag(PrivacyConsentGateConstant.ACCEPT_BUTTON_TAG).assertDoesNotExist()
        }

    @Test
    fun testNotAccepted_blocksContentAndAcceptButtonStartsDisabled() =
        runComposeUiTest {
            setContent {
                PrivacyConsentGate(
                    privacyConsentRepository = FakePrivacyConsentRepository(accepted = false),
                    policyContent = summaryPolicyContent,
                ) {
                    Text(GATED_CONTENT_TEXT)
                }
            }

            onNodeWithText(GATED_CONTENT_TEXT).assertDoesNotExist()
            onNodeWithTag(PrivacyConsentGateConstant.ACCEPT_BUTTON_TAG).assertIsNotEnabled()
        }

    @Test
    fun testCheckingBox_enablesAcceptButton() =
        runComposeUiTest {
            setContent {
                PrivacyConsentGate(
                    privacyConsentRepository = FakePrivacyConsentRepository(accepted = false),
                    policyContent = summaryPolicyContent,
                ) {
                    Text(GATED_CONTENT_TEXT)
                }
            }

            onNodeWithTag(PrivacyConsentGateConstant.CHECKBOX_TAG).performClick()

            onNodeWithTag(PrivacyConsentGateConstant.ACCEPT_BUTTON_TAG).assertIsEnabled()
        }

    @Test
    fun testAccepting_persistsAndRevealsContent() =
        runComposeUiTest {
            val fakeRepository = FakePrivacyConsentRepository(accepted = false)

            setContent {
                PrivacyConsentGate(privacyConsentRepository = fakeRepository, policyContent = summaryPolicyContent) {
                    Text(GATED_CONTENT_TEXT)
                }
            }

            onNodeWithTag(PrivacyConsentGateConstant.CHECKBOX_TAG).performClick()
            onNodeWithTag(PrivacyConsentGateConstant.ACCEPT_BUTTON_TAG).performClick()

            onNodeWithText(GATED_CONTENT_TEXT).assertExists()
            assertEquals(1, fakeRepository.markPrivacyPolicyAcceptedCallCount)
        }
}
