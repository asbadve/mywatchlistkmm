package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.runtime.Immutable

private object HeroScrimStopConstant {
    /** The wash never begins above this, so it cannot collide with the top wash. */
    const val EARLIEST_WASH_START = 0.28f

    /**
     * The shortest the wash may be, as a fraction of hero height.
     *
     * The alpha range is fixed at 0 to opaque, so the span is what sets how *steep* the fade is.
     * Left to follow the content, a tall TV hero fades over half the frame while a short movie hero
     * compresses the identical curve into a third - the same gradient, half again as steep, which
     * reads as a hard sweep rather than a wash. Holding a floor here gives every hero the same
     * rate, and the cost is only that a short hero's wash begins a little above its title, at an
     * alpha near zero where it is not noticeable.
     */
    const val MIN_WASH_SPAN = 0.50f

    /**
     * Where between the wash's start and the hero's bottom it reaches
     * [contentWashAlpha][HeroColors.contentWashAlpha].
     *
     * Half way down the content: the title sits above it and is large and bold enough to carry a
     * lighter wash, while the small print below sits at or past it.
     */
    const val FULL_STRENGTH_FRACTION = 0.5f

    /**
     * Where the content is assumed to start until it has been measured.
     *
     * Matches the tallest heroes rather than the average: guessing low costs a frame of slightly
     * over-washed artwork, guessing high costs a frame of unreadable title.
     */
    const val UNMEASURED_CONTENT_TOP = 0.50f

    /** Stops have to be strictly increasing, so a degenerate measurement still separates them. */
    const val MIN_SEPARATION = 0.02f
}

/**
 * Where the hero's content wash begins and where it reaches full strength, as fractions of hero
 * height.
 */
@Immutable
internal data class HeroScrimStops(
    /** Where the artwork stops being untouched - the top of the content. */
    val washStart: Float,
    /** Where the wash reaches its nominal strength, and from where it carries on to opaque. */
    val fullStrengthAt: Float,
) {
    companion object {
        /**
         * Places the wash against the content that actually has to sit on it.
         *
         * A fixed fraction cannot serve both hero shapes. A movie hero is a title, a facts row and
         * one button, and its content starts around 70% down; a TV hero with a status badge,
         * provider chips and a watch button starts nearer 50%. Tuned for the short one, the badge
         * on a tall hero ends up above the wash on bare artwork (this is what `Reacher` looked
         * like on device, 2026-08-15); tuned for the tall one, every short hero throws away a third
         * of its image for nothing.
         *
         * So the caller measures its content and the wash follows: it begins at the top of the
         * content and spreads from there to the bottom of the hero, leaving the artwork above it
         * untouched - subject to [MIN_WASH_SPAN][HeroScrimStopConstant.MIN_WASH_SPAN], which stops
         * a short hero compressing the fade into a steep sweep.
         *
         * [contentTopFraction] is 0 at the top of the hero and 1 at its bottom. Pure so the
         * geometry can be asserted without a device.
         */
        fun forContentTop(contentTopFraction: Float): HeroScrimStops {
            val washStart =
                contentTopFraction
                    .coerceAtMost(1f - HeroScrimStopConstant.MIN_WASH_SPAN)
                    .coerceAtLeast(HeroScrimStopConstant.EARLIEST_WASH_START)
            val fullStrengthAt = washStart + (1f - washStart) * HeroScrimStopConstant.FULL_STRENGTH_FRACTION
            return HeroScrimStops(washStart = washStart, fullStrengthAt = fullStrengthAt)
        }

        /** The stops to use before the hero has laid its content out. */
        fun unmeasured(): HeroScrimStops = forContentTop(HeroScrimStopConstant.UNMEASURED_CONTENT_TOP)
    }
}

/**
 * Smoothstep: eases in and out so the wash has no visible edge where it starts or where its slope
 * changes.
 *
 * A linear ramp is not smooth to the eye even though it is smooth on paper - the sudden change of
 * slope at each end reads as a line across the artwork, which is exactly what a hero scrim must not
 * have. This flattens the curve at both ends so each stop blends into the next.
 */
internal fun smoothStep(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

internal fun lerp(
    start: Float,
    end: Float,
    fraction: Float,
): Float = start + (end - start) * fraction
