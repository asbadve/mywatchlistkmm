package com.ajinkyabadve.kmmmywatchlist.core.ui.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ExpandableTextUiTest {
    @Test
    fun testLongText_togglesBetweenReadMoreAndReadLess() =
        runComposeUiTest {
            setContent { PhoneWidth { ExpandableText(LONG_TEXT, COLLAPSED_LINES, 14.sp, Color.Black) } }

            onNodeWithText(READ_MORE).performClick()
            onNodeWithText(READ_LESS).assertExists()
            onNodeWithText(READ_MORE).assertDoesNotExist()

            onNodeWithText(READ_LESS).performClick()
            onNodeWithText(READ_MORE).assertExists()
        }

    @Test
    fun testTappingTheExpandedText_collapsesIt() =
        runComposeUiTest {
            setContent { PhoneWidth { ExpandableText(LONG_TEXT, COLLAPSED_LINES, 14.sp, Color.Black) } }

            onNodeWithText(READ_MORE).performClick()
            onNodeWithText(LONG_TEXT).performClick()

            onNodeWithText(READ_MORE).assertExists()
            onNodeWithText(READ_LESS).assertDoesNotExist()
        }

    @Test
    fun testShortText_showsNoToggle() =
        runComposeUiTest {
            setContent { PhoneWidth { ExpandableText(SHORT_TEXT, COLLAPSED_LINES, 14.sp, Color.Black) } }

            onNodeWithText(SHORT_TEXT).assertExists()
            onNodeWithText(READ_MORE).assertDoesNotExist()
            onNodeWithText(READ_LESS).assertDoesNotExist()
        }

    // A fixed phone width, so whether the long text overflows doesn't depend on the test
    // window (desktop, iOS simulator, or a foldable emulator).
    @Composable
    private fun PhoneWidth(content: @Composable () -> Unit) {
        Box(modifier = Modifier.width(PHONE_WIDTH_DP.dp)) { content() }
    }

    private companion object {
        const val PHONE_WIDTH_DP = 360
        const val COLLAPSED_LINES = 2
        const val READ_MORE = "Read more"
        const val READ_LESS = "Read less"
        const val SHORT_TEXT = "Short."
        val LONG_TEXT = "A season overview long enough to need several lines. ".repeat(20)
    }
}
