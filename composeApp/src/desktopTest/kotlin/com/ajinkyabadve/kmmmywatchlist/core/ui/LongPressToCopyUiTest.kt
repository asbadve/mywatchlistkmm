package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.asAwtTransferable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.awt.datatransfer.DataFlavor
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TARGET_TAG = "longPressToCopyTarget"
private const val TARGET_TEXT = "The Dark Knight"
private const val BASELINE_CLIPBOARD_TEXT = "unrelated pre-existing clipboard content"

// In-memory fake so these tests never touch the real host clipboard - the desktop actual backs
// LocalClipboard with the real AWT system clipboard, which is unavailable on a headless CI runner
// (no X server) and throws/no-ops instead of storing text there. Lives in desktopTest because
// NativeClipboard is a different type per platform, and reading the text back goes through AWT.
private class FakeClipboard : Clipboard {
    private var stored: ClipEntry? = null

    override suspend fun getClipEntry(): ClipEntry? = stored

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        stored = clipEntry
    }

    override val nativeClipboard: NativeClipboard = Any()

    @OptIn(ExperimentalComposeUiApi::class)
    fun storedText(): String? = stored?.asAwtTransferable?.getTransferData(DataFlavor.stringFlavor) as? String

    fun seed(text: String) {
        stored = plainTextClipEntry(text)
    }
}

@OptIn(ExperimentalTestApi::class)
class LongPressToCopyUiTest {
    @Test
    fun testLongPress_copiesTextToClipboard() =
        runComposeUiTest {
            val clipboard = FakeClipboard()
            setContent {
                CompositionLocalProvider(LocalClipboard provides clipboard) {
                    Box(modifier = Modifier.size(48.dp).testTag(TARGET_TAG).longPressToCopy(TARGET_TEXT))
                }
            }

            onNodeWithTag(TARGET_TAG).performTouchInput { longClick() }

            assertEquals(TARGET_TEXT, clipboard.storedText())
        }

    @Test
    fun testPlainClick_doesNotCopyToClipboard() =
        runComposeUiTest {
            val clipboard = FakeClipboard()
            clipboard.seed(BASELINE_CLIPBOARD_TEXT)
            setContent {
                CompositionLocalProvider(LocalClipboard provides clipboard) {
                    Box(modifier = Modifier.size(48.dp).testTag(TARGET_TAG).longPressToCopy(TARGET_TEXT))
                }
            }

            onNodeWithTag(TARGET_TAG).performClick()

            assertEquals(BASELINE_CLIPBOARD_TEXT, clipboard.storedText())
        }
}
