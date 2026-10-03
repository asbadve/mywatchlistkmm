package com.ajinkyabadve.kmmmywatchlist.core.ui.carousel

import androidx.lifecycle.Lifecycle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoScrollCarouselLogicTest {
    @Test
    fun testIsAutoScrollAllowed_trueWhenEverySignalPermitsIt() {
        assertTrue(
            isAutoScrollAllowed(
                itemCount = MULTIPLE_ITEMS,
                reduceMotionEnabled = false,
                screenReaderActive = false,
                lifecycleState = Lifecycle.State.RESUMED,
            ),
        )
    }

    @Test
    fun testIsAutoScrollAllowed_falseWithOneOrFewerItems() {
        assertFalse(
            isAutoScrollAllowed(
                itemCount = SINGLE_ITEM,
                reduceMotionEnabled = false,
                screenReaderActive = false,
                lifecycleState = Lifecycle.State.RESUMED,
            ),
        )
    }

    @Test
    fun testIsAutoScrollAllowed_falseWhenReduceMotionEnabled() {
        assertFalse(
            isAutoScrollAllowed(
                itemCount = MULTIPLE_ITEMS,
                reduceMotionEnabled = true,
                screenReaderActive = false,
                lifecycleState = Lifecycle.State.RESUMED,
            ),
        )
    }

    @Test
    fun testIsAutoScrollAllowed_falseWhenScreenReaderActive() {
        assertFalse(
            isAutoScrollAllowed(
                itemCount = MULTIPLE_ITEMS,
                reduceMotionEnabled = false,
                screenReaderActive = true,
                lifecycleState = Lifecycle.State.RESUMED,
            ),
        )
    }

    @Test
    fun testIsAutoScrollAllowed_falseWhenAppNotStarted() {
        assertFalse(
            isAutoScrollAllowed(
                itemCount = MULTIPLE_ITEMS,
                reduceMotionEnabled = false,
                screenReaderActive = false,
                lifecycleState = Lifecycle.State.CREATED,
            ),
        )
    }

    @Test
    fun testShouldWrapToStart_trueWhenAdvanceDidNotMoveTheCarousel() {
        // Multi-browse carousel stuck at its last reachable focal item: advancing is a no-op.
        assertTrue(shouldWrapToStart(itemBefore = LAST_REACHABLE_ITEM, itemAfter = LAST_REACHABLE_ITEM, target = LAST_REACHABLE_ITEM + 1))
    }

    @Test
    fun testShouldWrapToStart_falseWhenAdvanceMovedTheCarousel() {
        assertFalse(shouldWrapToStart(itemBefore = MIDDLE_ITEM, itemAfter = MIDDLE_ITEM + 1, target = MIDDLE_ITEM + 1))
    }

    @Test
    fun testShouldWrapToStart_falseWhenAlreadyWrappingToZero() {
        // The modulo wrap already targeted 0 - no second scroll needed even if it hasn't moved yet.
        assertFalse(shouldWrapToStart(itemBefore = MULTIPLE_ITEMS - 1, itemAfter = MULTIPLE_ITEMS - 1, target = 0))
    }

    private companion object {
        const val MULTIPLE_ITEMS = 5
        const val SINGLE_ITEM = 1
        const val LAST_REACHABLE_ITEM = 3
        const val MIDDLE_ITEM = 1
    }
}
