package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * WCAG 2.x contrast ratio between two opaque colours, from 1.0 (identical) to 21.0 (black on white).
 *
 * Exists because the hero's light-theme bug was invisible to every other kind of test: a Compose UI
 * test asserts that a node exists, and white text on a white scrim exists perfectly well. The only
 * assertion that would have caught it is a numeric one about colour, which is this.
 */
internal fun contrastRatio(
    foreground: Color,
    background: Color,
): Float {
    val lighter = max(foreground.luminance(), background.luminance())
    val darker = min(foreground.luminance(), background.luminance())
    return (lighter + HeroContrastConstant.LUMINANCE_OFFSET) / (darker + HeroContrastConstant.LUMINANCE_OFFSET)
}

/** The colour a translucent [scrim] resolves to when laid over a backdrop of [backdrop]. */
internal fun scrimOver(
    scrim: Color,
    alpha: Float,
    backdrop: Color,
): Color = scrim.copy(alpha = alpha).compositeOver(backdrop)

internal object HeroContrastConstant {
    /** WCAG's constant, which keeps the ratio finite when one colour is pure black. */
    const val LUMINANCE_OFFSET = 0.05f

    /** WCAG AA for normal text. */
    const val AA_NORMAL_TEXT = 4.5f

    /** WCAG AA for large or bold text - what a scrim over unknown artwork can honestly promise. */
    const val AA_LARGE_TEXT = 3.0f
}
