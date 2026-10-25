package com.ajinkyabadve.kmmmywatchlist.testing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollToIndexAction

/**
 * Matches a screen's vertical lazy list, the one `performScrollToIndex` should drive.
 *
 * `hasScrollToIndexAction()` on its own also matches every horizontal rail (cast, similar,
 * recommendations) that is composed at the time. How many are composed depends on the window
 * size: on a phone-sized emulator the selector found 2-3 nodes and failed, while on a wide
 * foldable it found exactly one. Requiring a vertical scroll axis keeps the selector independent
 * of screen size.
 */
val isVerticalLazyList: SemanticsMatcher =
    hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
