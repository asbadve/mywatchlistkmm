package com.ajinkyabadve.kmmmywatchlist.design.timeline

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TimelineUiTest {
    @Test
    fun testSectionHeader_rendersTitle() =
        runComposeUiTest {
            setContent { TimelineSectionHeader(title = SECTION_TITLE) }

            onNodeWithText(SECTION_TITLE).assertExists()
        }

    @Test
    fun testEntryCard_rendersContentAndTappingInvokesOnClick() =
        runComposeUiTest {
            var clickCount = 0
            setContent {
                TimelineEntryCard(contentHeight = CONTENT_HEIGHT.dp, highlighted = true, onClick = { clickCount++ }) {
                    Text(ENTRY_TEXT)
                }
            }

            onNodeWithText(ENTRY_TEXT).performClick()

            assertEquals(1, clickCount)
        }

    private companion object {
        const val SECTION_TITLE = "October 2026"
        const val ENTRY_TEXT = "Lanterns"
        const val CONTENT_HEIGHT = 96
    }
}
