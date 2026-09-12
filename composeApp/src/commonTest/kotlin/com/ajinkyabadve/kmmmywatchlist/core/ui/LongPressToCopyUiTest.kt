package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
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

// In-memory fake so these tests never touch the real host clipboard - the desktop actual backs
// LocalClipboardManager with the real AWT system clipboard, which is unavailable on a headless CI
// runner (no X server) and throws/no-ops instead of storing text there.
private class FakeClipboardManager : ClipboardManager {
    private var stored: AnnotatedString? = null

    override fun setText(annotatedString: AnnotatedString) {
        stored = annotatedString
    }

    override fun getText(): AnnotatedString? = stored
}

@OptIn(ExperimentalTestApi::class)
class LongPressToCopyUiTest {
    @Test
    fun testLongPress_copiesTextToClipboard() =
        runComposeUiTest {
            val clipboardManager = FakeClipboardManager()
            setContent {
                CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                    Box(modifier = Modifier.size(48.dp).testTag(TARGET_TAG).longPressToCopy(TARGET_TEXT))
                }
            }

            onNodeWithTag(TARGET_TAG).performTouchInput { longClick() }

            assertEquals(TARGET_TEXT, clipboardManager.getText()?.text)
        }

    @Test
    fun testPlainClick_doesNotCopyToClipboard() =
        runComposeUiTest {
            val clipboardManager = FakeClipboardManager()
            clipboardManager.setText(AnnotatedString(BASELINE_CLIPBOARD_TEXT))
            setContent {
                CompositionLocalProvider(LocalClipboardManager provides clipboardManager) {
                    Box(modifier = Modifier.size(48.dp).testTag(TARGET_TAG).longPressToCopy(TARGET_TEXT))
                }
            }

            onNodeWithTag(TARGET_TAG).performClick()

            assertEquals(BASELINE_CLIPBOARD_TEXT, clipboardManager.getText()?.text)
        }
}
