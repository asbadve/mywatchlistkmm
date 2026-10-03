package com.ajinkyabadve.kmmmywatchlist.core.ui.carousel

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ajinkyabadve.kmmmywatchlist.isReducedMotionEnabled
import com.ajinkyabadve.kmmmywatchlist.isScreenReaderActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal object AutoScrollCarouselConstant {
    const val INTERVAL_MS = 4500L
}

/**
 * True while the carousel is allowed to auto-advance: more than one item to cycle through, the
 * OS's reduce-motion preference is off, no screen reader is active, and the app is at least
 * started (foregrounded). Extracted from [AutoScrollCarouselEffect] so this gating decision is
 * unit-testable without a Compose test harness.
 */
internal fun isAutoScrollAllowed(
    itemCount: Int,
    reduceMotionEnabled: Boolean,
    screenReaderActive: Boolean,
    lifecycleState: Lifecycle.State,
): Boolean =
    itemCount > 1 &&
        !reduceMotionEnabled &&
        !screenReaderActive &&
        lifecycleState.isAtLeast(Lifecycle.State.STARTED)

/**
 * Auto-advances [state] to the next item on a timer, wrapping back to 0 after the last one.
 * Skips a tick while the user is actively dragging/flinging ([CarouselState.isScrollInProgress]);
 * [isAutoScrollAllowed] gates whether the loop runs at all. Restarting the effect (its keys)
 * whenever that gate changes stops the loop immediately rather than only skipping its next tick,
 * and composition disposal (leaving the Trending tab) cancels it via structured concurrency with no
 * extra visibility plumbing needed.
 *
 * Tracks the lifecycle state by hand via [LifecycleEventObserver] rather than the newer
 * `androidx.lifecycle.compose.currentStateAsState()` extension - the latter needs the
 * `lifecycle-runtime-compose` artifact resolving in the IDE's editor classpath, which this
 * project's Android Studio setup was not picking up even after a clean Gradle sync (the
 * command-line build always resolved it correctly). `LocalLifecycleOwner`/`Lifecycle`/
 * `LifecycleEventObserver` come from the older, already-working `lifecycle-runtime`/compose-ui
 * classpath every other lifecycle-aware composable in this codebase already relies on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutoScrollCarouselEffect(
    state: CarouselState,
    itemCount: Int,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> lifecycleState = event.targetState }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val autoScrollAllowed =
        isAutoScrollAllowed(
            itemCount = itemCount,
            reduceMotionEnabled = isReducedMotionEnabled(),
            screenReaderActive = isScreenReaderActive(),
            lifecycleState = lifecycleState,
        )

    LaunchedEffect(state, itemCount, autoScrollAllowed) {
        if (!autoScrollAllowed) return@LaunchedEffect
        while (isActive) {
            delay(AutoScrollCarouselConstant.INTERVAL_MS)
            if (!state.isScrollInProgress) {
                val itemBefore = state.currentItem
                val target = (itemBefore + 1) % itemCount
                state.animateScrollToItem(target)
                if (shouldWrapToStart(itemBefore = itemBefore, itemAfter = state.currentItem, target = target)) {
                    state.animateScrollToItem(0)
                }
            }
        }
    }
}

/**
 * True when an auto-advance to [target] didn't move the carousel, so it should go back to item 0.
 *
 * A multi-browse carousel shows several items at once, so the last few can never become
 * [CarouselState.currentItem] - it runs out of room to scroll first. Advancing from there is a
 * no-op, and the `% itemCount` wrap never triggers because `currentItem + 1` never reaches
 * `itemCount`, leaving the carousel stuck at the end. [CarouselState] doesn't override
 * `canScrollForward` (it's always `true`), so "the advance didn't move it" is the only signal.
 */
internal fun shouldWrapToStart(
    itemBefore: Int,
    itemAfter: Int,
    target: Int,
): Boolean = target != 0 && itemAfter == itemBefore
