package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TARGET_TAG = "longPressToCopyTarget"
private const val TARGET_TEXT = "The Dark Knight"
private const val BASELINE_CLIPBOARD_TEXT = "unrelated pre-existing clipboard content"

@OptIn(ExperimentalTestApi::class)
class LongPressToCopyUiTest {
    @Test
    fun testLongPress_copiesTextToClipboard() =
        runComposeUiTest {
            lateinit var clipboardManager: ClipboardManager
            setContent {
                clipboardManager = LocalClipboardManager.current
                Box(modifier = Modifier.size(48.dp).testTag(TARGET_TAG).longPressToCopy(TARGET_TEXT))
            }

            onNodeWithTag(TARGET_TAG).performTouchInput { longClick() }

            assertEquals(TARGET_TEXT, clipboardManager.getText()?.text)
        }

    // Seeds a known baseline directly (not via TARGET_TEXT, which another test in this class
    // legitimately leaves on the clipboard) rather than asserting "empty" - the desktop actual
    // backs LocalClipboardManager with the real host AWT clipboard, which persists across tests
    // and isn't guaranteed empty to begin with.
    @Test
    fun testPlainClick_doesNotCopyToClipboard() =
        runComposeUiTest {
            lateinit var clipboardManager: ClipboardManager
            setContent {
                clipboardManager = LocalClipboardManager.current
                Box(modifier = Modifier.size(48.dp).testTag(TARGET_TAG).longPressToCopy(TARGET_TEXT))
            }
            clipboardManager.setText(AnnotatedString(BASELINE_CLIPBOARD_TEXT))

            onNodeWithTag(TARGET_TAG).performClick()

            assertEquals(BASELINE_CLIPBOARD_TEXT, clipboardManager.getText()?.text)
        }
}
