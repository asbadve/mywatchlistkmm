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
            state.nestedScrollConnection.onPreScroll(
                available = Offset(0f, -30f),
                source = NestedScrollSource.UserInput,
            )

        assertEquals(Offset.Zero, consumed)
    }

    private fun CollapsibleBarState.scrollBy(deltaY: Float) {
        nestedScrollConnection.onPreScroll(
            available = Offset(0f, deltaY),
            source = NestedScrollSource.UserInput,
        )
    }
}
