package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DeviceTransferContentUiTest {
    @Test
    fun testShowingCode_rendersTheQrCodeAndInstructions() =
        runComposeUiTest {
            setContent { SendTransferContent(SendTransferState.ShowingCode(CODE, SHORT_CODE), {}, {}, {}, {}) }

            onNodeWithContentDescription(QR_DESCRIPTION).assertExists()
            onNodeWithText(SHORT_CODE).assertExists()
            onNodeWithText(VALIDITY).assertExists()
        }

    @Test
    fun testRequest_namesTheDeviceAndSendCallsApprove() =
        runComposeUiTest {
            var approved = 0
            var declined = 0
            setContent { SendTransferContent(SendTransferState.Request(DEVICE), { approved++ }, { declined++ }, {}, {}) }

            onNodeWithText(REQUEST_MESSAGE).assertExists()
            onNodeWithText(SEND).performClick()
            onNodeWithText(DONT_SEND).performClick()

            assertEquals(1, approved)
            assertEquals(1, declined)
        }

    @Test
    fun testNoNetwork_explainsAndOffersRetry() =
        runComposeUiTest {
            var retried = 0
            setContent { SendTransferContent(SendTransferState.NoNetwork, {}, {}, { retried++ }, {}) }

            onNodeWithText(NO_NETWORK, substring = true).assertExists()
            onNodeWithText(TRY_AGAIN).performClick()

            assertEquals(1, retried)
        }

    @Test
    fun testScanning_showsTheScannerSlot() =
        runComposeUiTest {
            setContent {
                ReceiveTransferContent(
                    ReceiveTransferState.Scanning,
                    scanner = { Box(Modifier.testTag(SCANNER_TAG)) },
                    onRetry = {},
                    onClose = {},
                    showScanner = true,
                )
            }

            onNodeWithTag(SCANNER_TAG).assertExists()
        }

    @Test
    fun testCodeEntry_passesTheTypedCodeOn() =
        runComposeUiTest {
            val entered = mutableListOf<String>()
            setContent {
                ReceiveTransferContent(
                    ReceiveTransferState.Scanning,
                    scanner = {},
                    onRetry = {},
                    onClose = {},
                    showScanner = true,
                    onCodeEntered = { entered.add(it) },
                )
            }

            onNodeWithText(CODE_LABEL).performScrollTo().performTextInput(SHORT_CODE)
            onNodeWithText(CONNECT).performScrollTo().performClick()

            assertEquals(listOf(SHORT_CODE), entered)
        }

    @Test
    fun testWithoutACamera_asksForTheTypedCodeAndShowsNoScanner() =
        runComposeUiTest {
            setContent {
                ReceiveTransferContent(
                    ReceiveTransferState.Scanning,
                    scanner = { Box(Modifier.testTag(SCANNER_TAG)) },
                    onRetry = {},
                    onClose = {},
                    showScanner = false,
                )
            }

            onNodeWithTag(SCANNER_TAG).assertDoesNotExist()
            onNodeWithText(TYPE_INSTRUCTIONS, substring = true).assertExists()
            onNodeWithText(CODE_LABEL).assertExists()
        }

    @Test
    fun testUnreachable_mentionsWifiAndOffersAnotherScan() =
        runComposeUiTest {
            var retried = 0
            setContent { ReceiveTransferContent(ReceiveTransferState.Unreachable, scanner = {}, onRetry = { retried++ }, onClose = {}) }

            onNodeWithText(UNREACHABLE, substring = true).assertExists()
            onNodeWithText(SCAN_AGAIN).performClick()

            assertEquals(1, retried)
        }

    private companion object {
        const val CODE = "mwlxfer1;192.168.1.23;40123;AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=;1791000000"
        const val DEVICE = "iPhone 17"
        const val QR_DESCRIPTION = "Transfer code"
        const val VALIDITY = "The code works once and expires in 5 minutes."
        const val REQUEST_MESSAGE = "Send your backup to iPhone 17?"
        const val SEND = "Send"
        const val DONT_SEND = "Don't send"
        const val NO_NETWORK = "Connect to Wi-Fi"
        const val TRY_AGAIN = "Try again"
        const val SCANNER_TAG = "scanner"
        const val UNREACHABLE = "same Wi-Fi"
        const val SCAN_AGAIN = "Scan again"
        const val SHORT_CODE = "7K3M-Q9XA-4RTB-W2HN-8CFD"
        const val CODE_LABEL = "Or type the code from the other device"
        const val CONNECT = "Connect"
        const val TYPE_INSTRUCTIONS = "Type the code shown under its QR code"
    }
}
