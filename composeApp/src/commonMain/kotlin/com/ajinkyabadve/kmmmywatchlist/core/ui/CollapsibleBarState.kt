package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Drives an "enter always" collapse for a bar that Material3's own scroll behaviours do not cover -
 * namely the app-level bottom [androidx.compose.material3.NavigationBar] and the top bars that
 * MovieDetailScreen/TvDetailScreen render inside their content (rather than in `Scaffold(topBar =)`)
 * to keep the 50/50 split measurable.
 *
 * Scrolling down slides the bar out; scrolling up brings it straight back, at any point in the list.
 * [TopAppBarDefaults.enterAlwaysScrollBehavior][androidx.compose.material3.TopAppBarDefaults] is the
 * better tool wherever a bar really does live in `Scaffold(topBar =)`; this exists for the cases
 * where it cannot be used.
 */
@Stable
class CollapsibleBarState {
    /** Measured bar height in pixels; 0 until the bar has been laid out once. */
    var heightPx by mutableFloatStateOf(0f)
        internal set

    /** How far the bar is currently displaced, in pixels. `-heightPx` = fully hidden, `0` = shown. */
    var offsetPx by mutableFloatStateOf(0f)
        internal set

    /** Fraction of the bar currently hidden, `0f` (fully visible) to `1f` (fully collapsed). */
    val collapsedFraction: Float
        get() {
            // Negating a zero offset would otherwise yield -0.0f, which `==` treats as zero but
            // `equals` does not - enough to surprise anything comparing or keying off this value.
            if (heightPx == 0f || offsetPx == 0f) return 0f
            return (-offsetPx / heightPx).coerceIn(0f, 1f)
        }

    /**
     * Snaps the bar back into view. Call when the destination changes, otherwise a screen left
     * mid-scroll would open the next one with its bar still hidden.
     */
    fun reset() {
        offsetPx = 0f
    }

    /**
     * Consumes nothing - it only observes, so the list still scrolls normally.
     *
     * Deliberately `onPostScroll` reading [consumed][NestedScrollConnection.onPostScroll], not
     * `onPreScroll` reading `available`. `available` is what the *gesture* offered, which is
     * non-zero even when the list cannot move: on a screen whose content is shorter than the
     * viewport, a drag would hide both bars while nothing scrolled, leaving a screen with no
     * navigation and no way to have caused it. `consumed` is what the list actually scrolled, so a
     * list that cannot move leaves the bars alone.
     *
     * The trade-off is that the bar now tracks list movement rather than finger movement. Where a
     * list can only scroll a little, the bar collapses only that far and sits partly hidden at the
     * end of the scroll - which is honest about how much room there was, and undoes itself on the
     * way back up.
     */
    val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                offsetPx = (offsetPx + consumed.y).coerceIn(-heightPx, 0f)
                return Offset.Zero
            }
        }
}

@Composable
fun rememberCollapsibleBarState(): CollapsibleBarState = remember { CollapsibleBarState() }

/**
 * Measures the bar into [state] and translates it off the top edge as it collapses. For a top bar.
 */
fun Modifier.collapsingTopBar(state: CollapsibleBarState): Modifier =
    onSizeChanged { state.heightPx = it.height.toFloat() }
        .offset { IntOffset(x = 0, y = state.offsetPx.roundToInt()) }

/**
 * For a bar that occupies space in a layout rather than floating over it: as the bar collapses it
 * reports a smaller and smaller height, so whatever sits below it (Scaffold's content, a grid under
 * a tab row) rises to fill the space instead of a gap opening up. The bar itself slides upward out
 * of its shrinking box, which [clipToBounds] then hides.
 *
 * Contrast [collapsingTopBar], which only translates - correct for an overlay bar that must not
 * disturb the layout underneath it, wrong for one that is part of the flow.
 *
 * Must not be applied inside a fixed height (`Modifier.height(x).collapsingHeader(state)`): the
 * outer height wins and the bar goes on reporting its full size however far it has collapsed. Put
 * it to the left of any sizing modifier instead.
 */
fun Modifier.collapsingHeader(state: CollapsibleBarState): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        state.heightPx = placeable.height.toFloat()
        val visibleHeight = (placeable.height + state.offsetPx).roundToInt().coerceIn(0, placeable.height)
        layout(placeable.width, visibleHeight) {
            placeable.place(x = 0, y = state.offsetPx.roundToInt())
        }
    }

/**
 * Mirror of [collapsingHeader] for a bar pinned to the bottom. The bar keeps its own top edge and
 * simply reports less height; because a bottom bar is laid out against the bottom of its parent,
 * shrinking it is what walks it off the bottom of the screen.
 *
 * [minVisibleHeight] is how much of the bar refuses to collapse, and under edge-to-edge it is not
 * optional: Material's NavigationBar is what consumes the bottom system inset, so a bar allowed to
 * reach zero height takes that inset with it and drops the content under the gesture bar, where it
 * is neither fully visible nor reliably tappable. Pass the navigation-bar inset so the collapse
 * gives back the bar's own chrome and nothing more.
 */
fun Modifier.collapsingFooter(
    state: CollapsibleBarState,
    minVisibleHeight: Dp = 0.dp,
): Modifier =
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        state.heightPx = placeable.height.toFloat()
        val floor = minVisibleHeight.roundToPx().coerceAtMost(placeable.height)
        val visibleHeight = (placeable.height + state.offsetPx).roundToInt().coerceIn(floor, placeable.height)
        layout(placeable.width, visibleHeight) {
            // Slide the bar down out of the shrinking box rather than letting the box close over
            // it. Pinning the bar's bottom edge (y = visibleHeight - height) keeps it at a fixed
            // screen position and simply clips more of it away each frame - the icons never move,
            // they are guillotined where they stand, which reads as the bar blinking out rather
            // than leaving.
            //
            // Travel is scaled by [minVisibleHeight] so the bar clears the bottom edge exactly as
            // the box reaches its floor. Plain `y = 0` slides correctly but stops a few pixels
            // short, leaving the tops of the icons showing under the gesture bar.
            val collapsibleHeight = (placeable.height - floor).toFloat()
            val progress = if (collapsibleHeight <= 0f) 0f else (placeable.height - visibleHeight) / collapsibleHeight
            placeable.place(x = 0, y = (floor * progress).roundToInt())
        }
    }
