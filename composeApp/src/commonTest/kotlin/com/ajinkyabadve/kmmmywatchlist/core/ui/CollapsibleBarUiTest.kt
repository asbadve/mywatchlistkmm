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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
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

/**
 * Exercises the collapse through a real scrolling list rather than the connection alone, so a
 * regression in how the modifiers are wired (measuring the bar, dispatching nested scroll) fails
 * here even when [CollapsibleBarStateTest] still passes.
 */
@OptIn(ExperimentalTestApi::class)
class CollapsibleBarUiTest {
    @Composable
    private fun CollapsibleBarHarness(
        state: CollapsibleBarState,
        itemCount: Int = ITEM_COUNT,
    ) {
        Box(modifier = Modifier.fillMaxSize().nestedScroll(state.nestedScrollConnection)) {
            LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIST_TAG)) {
                items(itemCount) { index -> Text("item $index", modifier = Modifier.height(40.dp)) }
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

    @Composable
    private fun CollapsingFooterHarness(state: CollapsibleBarState) {
        Column(modifier = Modifier.fillMaxSize().nestedScroll(state.nestedScrollConnection)) {
            LazyColumn(modifier = Modifier.weight(1f).testTag(LIST_TAG)) {
                items(ITEM_COUNT) { index -> Text("item $index", modifier = Modifier.height(40.dp)) }
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(FOOTER_TAG)
                        .collapsingFooter(state, minVisibleHeight = FOOTER_FLOOR)
                        .height(80.dp),
            ) {
                Text("Nav", modifier = Modifier.testTag(FOOTER_CONTENT_TAG))
            }
        }
    }

    /**
     * Under edge-to-edge the bottom bar is what consumes the navigation-bar inset, so it must stop
     * collapsing at that inset. A bar that reached zero would take the inset with it and leave the
     * content sitting under the gesture bar.
     */
    @Test
    fun testCollapsingFooterStopsAtItsMinimumVisibleHeight() =
        runComposeUiTest {
            val state = CollapsibleBarState()
            setContent { CollapsingFooterHarness(state) }

            repeat(3) { onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() } }

            val footerHeight = onNodeWithTag(FOOTER_TAG).getBoundsInRoot().height
            assertEquals(
                FOOTER_FLOOR,
                footerHeight,
                "footer should hold at the inset floor rather than collapsing away entirely",
            )
        }

    /**
     * A screen whose content fits without scrolling must keep its bars, however hard it is dragged.
     * Collapsing there strands the user with no navigation on a screen that never moved - which is
     * what EpisodeDetail did until the connection switched from the offered delta to the consumed
     * one. The unit test pins the arithmetic; this pins the wiring, with a list that genuinely
     * cannot scroll.
     */
    @Test
    fun testAListThatCannotScrollLeavesTheBarAlone() =
        runComposeUiTest {
            val state = CollapsibleBarState()
            setContent { CollapsibleBarHarness(state, itemCount = SHORT_ITEM_COUNT) }

            assertTrue(state.heightPx > 0f, "bar was never measured")

            repeat(3) { onNodeWithTag(LIST_TAG).performTouchInput { swipeUp() } }

            assertEquals(
                0f,
                state.collapsedFraction,
                "a list with nothing to scroll must not collapse the bar",
            )
        }

    /**
     * The footer has to *leave*, not be clipped where it stands.
     *
     * Pinning the bar's bottom edge to the shrinking box holds it at a fixed screen position and
     * simply cuts more off the top each frame - the height assertions above all still pass, but on
     * a device it reads as the bar blinking out rather than sliding away. Watching the content
     * inside the bar is what tells the two apart: if it slid, the label moved down.
     */
    @Test
    fun testCollapsingFooterSlidesItsContentDownRatherThanClippingItInPlace() =
        runComposeUiTest {
            val state = CollapsibleBarState()
            setContent { CollapsingFooterHarness(state) }

            val labelTopBefore = onNodeWithTag(FOOTER_CONTENT_TAG).getBoundsInRoot().top

            // Driven directly and only part of the way, for two reasons: a full collapse clips the
            // label out entirely and `getBoundsInRoot` then reports zero rather than an off-screen
            // position, and a swipe's exact distance is not worth depending on here.
            state.nestedScrollConnection.onPostScroll(
                consumed = Offset(0f, -state.heightPx / 3f),
                available = Offset.Zero,
                source = NestedScrollSource.UserInput,
            )
            waitForIdle()

            val labelTopAfter = onNodeWithTag(FOOTER_CONTENT_TAG).getBoundsInRoot().top
            assertTrue(
                labelTopAfter > labelTopBefore,
                "footer content should travel downward as the bar collapses ($labelTopBefore -> $labelTopAfter)",
            )
        }

    private companion object {
        const val LIST_TAG = "list"
        const val HEADER_TAG = "header"
        const val FOOTER_TAG = "footer"
        const val FOOTER_CONTENT_TAG = "footerContent"
        val FOOTER_FLOOR = 24.dp

        /** Enough items to overflow the test root many times over, so the list definitely scrolls. */
        const val ITEM_COUNT = 100

        /** Few enough that the list fits the root with room to spare, so it cannot scroll at all. */
        const val SHORT_ITEM_COUNT = 2
    }
}
