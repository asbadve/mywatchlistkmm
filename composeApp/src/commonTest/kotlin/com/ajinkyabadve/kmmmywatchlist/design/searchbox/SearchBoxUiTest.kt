package com.ajinkyabadve.kmmmywatchlist.design.searchbox

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SearchBoxUiTest {
    @Test
    fun testSearchBox_isASingleButtonNamedByItsHint() =
        runComposeUiTest {
            setContent { SearchBox(hint = HINT, onClick = {}) }

            onNodeWithText(HINT).assertExists()
            // The icon used to be its own no-op IconButton, labelled "Localized description".
            onAllNodes(hasClickAction()).assertCountEquals(1)
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
                .assertCountEquals(0)
        }

    @Test
    fun testSearchBox_tapOnSearchIcon_triggersOnClick() =
        runComposeUiTest {
            var clicked = false
            setContent { SearchBox(hint = HINT, onClick = { clicked = true }) }

            onAllNodes(hasClickAction())[0].performTouchInput {
                click(Offset(x = ICON_CENTER_FROM_START.dp.toPx(), y = centerY))
            }

            assertTrue(clicked)
        }

    @Test
    fun testSearchBox_tapOnHint_triggersOnClick() =
        runComposeUiTest {
            var clicked = false
            setContent { SearchBox(hint = HINT, onClick = { clicked = true }) }

            onAllNodesWithText(HINT)[0].performTouchInput { click() }

            assertTrue(clicked)
        }

    private companion object {
        const val HINT = "Search for Movies & Tv shows"

        /** Centre of the 48dp icon slot pinned to the pill's start edge. */
        const val ICON_CENTER_FROM_START = 24
    }
}
