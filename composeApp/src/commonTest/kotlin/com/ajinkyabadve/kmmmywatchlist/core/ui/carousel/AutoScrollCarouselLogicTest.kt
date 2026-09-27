package com.ajinkyabadve.kmmmywatchlist.core.ui.carousel

import androidx.lifecycle.Lifecycle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoScrollCarouselLogicTest {
    private companion object {
        const val MULTIPLE_ITEMS = 5
        const val SINGLE_ITEM = 1
    }

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
}
