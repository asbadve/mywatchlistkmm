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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val LIST_TAG = "list"
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
}
