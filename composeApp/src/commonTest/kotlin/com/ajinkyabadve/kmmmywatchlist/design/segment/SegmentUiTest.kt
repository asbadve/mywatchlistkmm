package com.ajinkyabadve.kmmmywatchlist.design.segment

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SegmentUiTest {
    private val options = listOf("Movies", "TV Shows", "People")

    @Test
    fun testSingleChoiceSegment_rendersAllOptionLabels() =
        runComposeUiTest {
            setContent {
                SingleChoiceSegment(
                    modifier = Modifier,
                    onOptionSelected = {},
                    options = options,
                    selectedIndex = 0,
                )
            }

            options.forEach { label -> onNodeWithText(label).assertExists() }
        }

    @Test
    fun testSingleChoiceSegment_reflectsSelectedIndexInSemantics() =
        runComposeUiTest {
            setContent {
                SingleChoiceSegment(
                    modifier = Modifier,
                    onOptionSelected = {},
                    options = options,
                    selectedIndex = 1,
                )
            }

            onNodeWithText("TV Shows").assertIsSelected()
            onNodeWithText("Movies").assertIsNotSelected()
            onNodeWithText("People").assertIsNotSelected()
        }

    @Test
    fun testSingleChoiceSegment_clickingUnselectedOption_invokesCallbackWithIndex() =
        runComposeUiTest {
            var selectedIndex: Int? = null
            setContent {
                SingleChoiceSegment(
                    modifier = Modifier,
                    onOptionSelected = { selectedIndex = it },
                    options = options,
                    selectedIndex = 0,
                )
            }

            onNodeWithText("People").performClick()
            assertEquals(2, selectedIndex)
        }
}
