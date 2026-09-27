package com.ajinkyabadve.kmmmywatchlist.core.ui.carousel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class AutoScrollCarouselEffectUiTest {
    private companion object {
        // Wide enough, with enough items, that the carousel genuinely overflows the test
        // window's viewport - a carousel that already fits entirely on screen has nowhere to
        // scroll into, and animateScrollToItem is then a no-op regardless of what item it targets.
        const val ITEM_COUNT = 10
        const val ITEM_WIDTH_DP = 400

        // +3s beyond the interval, not just past it - animateScrollToItem's own spring animation
        // needs real virtual time on top of the delay to settle at the new page.
        const val ADVANCE_PAST_INTERVAL_MS = AutoScrollCarouselConstant.INTERVAL_MS + 3000L
        const val CAROUSEL_TAG = "carousel"
    }

    @Composable
    private fun resumedLifecycleOwner(): LifecycleOwner =
        remember {
            object : LifecycleOwner {
                override val lifecycle: Lifecycle =
                    LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
            }
        }

    /**
     * Renders the same [HorizontalMultiBrowseCarousel] shape [TrendingScreenTab]'s production
     * `TrendingMediaCarousel` uses - a bare [Box] item, unlike [Modifier.scrollable] on its own,
     * gives [CarouselState] the real Pager layout `animateScrollToItem` needs to actually move
     * [CarouselState.currentItem] (an unattached state has no viewport/item-size info to compute a
     * scroll distance against).
     */
    @Composable
    private fun TestCarousel(state: CarouselState) {
        HorizontalMultiBrowseCarousel(
            state = state,
            modifier = Modifier.fillMaxWidth().testTag(CAROUSEL_TAG),
            preferredItemWidth = ITEM_WIDTH_DP.dp,
            itemSpacing = 0.dp,
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(modifier = Modifier.fillMaxHeight().width(ITEM_WIDTH_DP.dp))
        }
    }

    @Test
    fun testAutoScrollCarouselEffect_advancesAfterIntervalWithNoInteraction() =
        runComposeUiTest {
            lateinit var state: CarouselState
            setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides resumedLifecycleOwner()) {
                    state = rememberCarouselState { ITEM_COUNT }
                    AutoScrollCarouselEffect(state = state, itemCount = ITEM_COUNT)
                    TestCarousel(state = state)
                }
            }

            mainClock.autoAdvance = false
            waitForIdle()
            val startingItem = state.currentItem

            mainClock.advanceTimeBy(ADVANCE_PAST_INTERVAL_MS)
            waitForIdle()

            // Not an exact index: the carousel's own initial-layout settling can consume some
            // virtual time before this point, so more than one interval may already have elapsed -
            // what matters here is that it moved with no interaction, not by exactly one step.
            assertNotEquals(startingItem, state.currentItem)
        }

    @Test
    fun testAutoScrollCarouselEffect_doesNotAdvanceWhileDragInProgress() =
        runComposeUiTest {
            lateinit var state: CarouselState
            setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides resumedLifecycleOwner()) {
                    state = rememberCarouselState { ITEM_COUNT }
                    AutoScrollCarouselEffect(state = state, itemCount = ITEM_COUNT)
                    TestCarousel(state = state)
                }
            }

            mainClock.autoAdvance = false
            waitForIdle()
            val startingItem = state.currentItem

            // A single synthetic down()+moveBy() only pulses CarouselState.isScrollInProgress
            // true for the instant it's dispatched, not for as long as the pointer is nominally
            // still down - unreliable to line up with the interval tick in a virtual-clock test.
            // Driving ScrollableState.scroll() directly holds isScrollInProgress true for a
            // controlled span, exercising the exact gate AutoScrollCarouselEffect checks.
            val dragJob =
                CoroutineScope(Dispatchers.Unconfined).launch {
                    state.scroll { awaitCancellation() }
                }
            waitForIdle()

            mainClock.advanceTimeBy(ADVANCE_PAST_INTERVAL_MS)
            waitForIdle()

            assertEquals(startingItem, state.currentItem)

            dragJob.cancel()
        }
}
