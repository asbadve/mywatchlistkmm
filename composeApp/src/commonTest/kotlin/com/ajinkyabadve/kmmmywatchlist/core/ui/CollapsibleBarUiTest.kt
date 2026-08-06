package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val LIST_TAG = "list"
private const val HEADER_TAG = "header"
private const val ITEM_COUNT = 100

/**
 * Exercises the collapse through a real scrolling list rather than the connection alone, so a
 * regression in how the modifiers are wired (measuring the bar, dispatching nested scroll) fails
 * here even when [CollapsibleBarStateTest] still passes.
 */
@OptIn(ExperimentalTestApi::class)
class CollapsibleBarUiTest {
    @Composable
    private fun CollapsibleBarHarness(state: CollapsibleBarState) {
        Box(modifier = Modifier.fillMaxSize().nestedScroll(state.nestedScrollConnection)) {
            LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST_TAG)) {
                items(ITEM_COUNT) { index -> Text("item $index", modifier = Modifier.height(40.dp)) }
            }
            Column(modifier = Modifier.fillMaxWidth().height(64.dp).collapsingTopBar(state)) {
                Text("Toolbar")
            }
        }
    }

    @Test
    fun testScrollingListDownCollapsesBarAndScrollingBackUpRevealsIt() =
        runComposeUiTest {
            val state = CollapsibleBarState()
            setContent { CollapsibleBarHarness(state) }

            // The bar has to have been measured before any of this means anything.
            assertTrue(state.heightPx > 0f, "bar was never measured")
            assertEquals(0f, state.collapsedFraction, "bar should start fully visible")

            onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }
            assertTrue(
                state.collapsedFraction > 0f,
                "scrolling down should have collapsed the bar, fraction was ${state.collapsedFraction}",
            )

            onNodeWithTag(LIST_TAG).performTouchInput { swipeDown() }
            assertEquals(0f, state.collapsedFraction, "scrolling back up should have revealed the bar")
        }

    @Composable
    private fun CollapsingHeaderHarness(state: CollapsibleBarState) {
        Column(modifier = Modifier.fillMaxSize().nestedScroll(state.nestedScrollConnection)) {
            // collapsingHeader has to sit outside the fixed height, or that height wins and the
            // node keeps reporting its full size however far the bar has collapsed.
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(HEADER_TAG)
                        .collapsingHeader(state)
                        .height(64.dp),
            ) {
                Text("Toolbar")
            }
            LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST_TAG)) {
                items(ITEM_COUNT) { index -> Text("item $index", modifier = Modifier.height(40.dp)) }
            }
        }
    }

    /**
     * The in-flow variant has to give its space back, not just slide: a header that kept reporting
     * full height while hidden would leave a blank strip where it used to be.
     */
    @Test
    fun testCollapsingHeaderYieldsItsSpaceToTheContentBelow() =
        runComposeUiTest {
            val state = CollapsibleBarState()
            setContent { CollapsingHeaderHarness(state) }

            val listTopBefore = onNodeWithTag(LIST_TAG).getBoundsInRoot().top
            val headerHeightBefore = onNodeWithTag(HEADER_TAG).getBoundsInRoot().height

            onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() }

            val headerHeightAfter = onNodeWithTag(HEADER_TAG).getBoundsInRoot().height
            val listTopAfter = onNodeWithTag(LIST_TAG).getBoundsInRoot().top
            assertTrue(
                headerHeightAfter < headerHeightBefore,
                "header should report less height once collapsed ($headerHeightBefore -> $headerHeightAfter)",
            )
            assertTrue(
                listTopAfter < listTopBefore,
                "content below should rise into the freed space ($listTopBefore -> $listTopAfter)",
            )
        }
}
