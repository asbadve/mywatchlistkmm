package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.test.Test
import kotlin.test.assertEquals

private const val BAR_HEIGHT = 100f

class CollapsibleBarStateTest {
    private fun stateWithBar(): CollapsibleBarState = CollapsibleBarState().also { it.heightPx = BAR_HEIGHT }

    /** Scrolling the list down (negative delta) slides the bar out by the same amount. */
    @Test
    fun testScrollingDownCollapsesTheBar() {
        val state = stateWithBar()

        state.scrollBy(-30f)

        assertEquals(-30f, state.offsetPx)
        assertEquals(0.3f, state.collapsedFraction)
    }

    /** The bar never slides further than its own height, however far the list scrolls. */
    @Test
    fun testCollapseIsClampedToBarHeight() {
        val state = stateWithBar()

        state.scrollBy(-500f)

        assertEquals(-BAR_HEIGHT, state.offsetPx)
        assertEquals(1f, state.collapsedFraction)
    }

    /**
     * The "enter always" half of the behaviour: scrolling up brings the bar straight back rather
     * than waiting for the list to reach the top.
     */
    @Test
    fun testScrollingBackUpRevealsTheBarImmediately() {
        val state = stateWithBar()
        state.scrollBy(-BAR_HEIGHT)

        state.scrollBy(40f)

        assertEquals(-60f, state.offsetPx)
    }

    /** Revealing never overshoots into a gap above the bar. */
    @Test
    fun testRevealIsClampedToFullyVisible() {
        val state = stateWithBar()
        state.scrollBy(-20f)

        state.scrollBy(500f)

        assertEquals(0f, state.offsetPx)
        assertEquals(0f, state.collapsedFraction)
    }

    /** Leaving a screen mid-scroll must not carry a hidden bar into the next one. */
    @Test
    fun testResetRestoresTheBar() {
        val state = stateWithBar()
        state.scrollBy(-BAR_HEIGHT)

        state.reset()

        assertEquals(0f, state.offsetPx)
    }

    /** Before the bar has been laid out there is no height to divide by. */
    @Test
    fun testCollapsedFractionIsZeroBeforeMeasurement() {
        assertEquals(0f, CollapsibleBarState().collapsedFraction)
    }

    /** The connection only observes; the list must still receive the whole delta. */
    @Test
    fun testConnectionConsumesNothing() {
        val state = stateWithBar()

        val consumed =
            state.nestedScrollConnection.onPostScroll(
                consumed = Offset(0f, -30f),
                available = Offset.Zero,
                source = NestedScrollSource.UserInput,
            )

        assertEquals(Offset.Zero, consumed)
    }

    /**
     * The screen-with-nothing-to-scroll case. A list shorter than the viewport consumes none of the
     * drag, and a bar that collapsed anyway would leave the user with no navigation on a screen that
     * never moved. Found on EpisodeDetail during the 2026-08-15 device pass.
     */
    @Test
    fun testADragThatScrollsNothingLeavesTheBarAlone() {
        val state = stateWithBar()

        // What a non-scrollable list reports: the gesture offered plenty, the list took none of it.
        state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, -400f),
            source = NestedScrollSource.UserInput,
        )

        assertEquals(0f, state.offsetPx)
        assertEquals(0f, state.collapsedFraction)
    }

    /**
     * A list with only a little room left collapses the bar only that far, rather than by the whole
     * gesture - the bar tracks the list, not the finger.
     */
    @Test
    fun testTheBarFollowsWhatTheListActuallyScrolled() {
        val state = stateWithBar()

        state.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, -25f),
            available = Offset(0f, -375f),
            source = NestedScrollSource.UserInput,
        )

        assertEquals(-25f, state.offsetPx)
    }

    private fun CollapsibleBarState.scrollBy(deltaY: Float) {
        nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, deltaY),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )
    }
}
