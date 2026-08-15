package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The hero's content wash follows the content instead of sitting at a fixed fraction.
 *
 * A fixed fraction cannot serve both hero shapes at once, and the failure is asymmetric: tuned for
 * short movie heroes, a TV hero's status badge ends up above the wash on bare artwork; tuned for
 * tall ones, every movie hero throws away a third of its image. These assert the geometry that
 * removes the choice.
 */
class HeroScrimStopsTest {
    /**
     * A short hero would compress the whole 0-to-opaque fade into the last third of the frame,
     * which reads as a steep sweep rather than a wash - the movie hero next to the TV one on
     * device, 2026-08-15. The minimum span holds the fade to the same rate as a tall hero's.
     */
    @Test
    fun testAShortHerosWashIsNotCompressedIntoSteepness() {
        val short = HeroScrimStops.forContentTop(SHORT_HERO_CONTENT_TOP)
        val tall = HeroScrimStops.forContentTop(TALL_HERO_CONTENT_TOP)

        assertTrue(
            short.washStart < SHORT_HERO_CONTENT_TOP,
            "the wash should start above short content rather than crushing the fade (was ${short.washStart})",
        )
        assertEquals(
            1f - short.washStart,
            1f - tall.washStart,
            absoluteTolerance = SPAN_TOLERANCE,
            message = "both hero shapes should fade at the same rate",
        )
    }

    /** The `Reacher` case: badge, title, facts, network, next episode, chips and a button. */
    @Test
    fun testTallContentIsFullyCoveredByTheWash() {
        val stops = HeroScrimStops.forContentTop(TALL_HERO_CONTENT_TOP)

        assertEquals(TALL_HERO_CONTENT_TOP, stops.washStart)
        assertTrue(stops.fullStrengthAt > stops.washStart, "the wash must keep building below where it starts")
        assertTrue(stops.fullStrengthAt < 1f, "and must reach its nominal strength before the hero ends")
    }

    /**
     * Past the minimum span the wash still follows the content, which is what keeps a very tall
     * hero's topmost element from sitting above it.
     */
    @Test
    fun testTallerContentStillPullsTheWashFurtherUpTheHero() {
        val tall = HeroScrimStops.forContentTop(TALL_HERO_CONTENT_TOP)
        val taller = HeroScrimStops.forContentTop(VERY_TALL_HERO_CONTENT_TOP)

        assertTrue(
            taller.washStart < tall.washStart,
            "taller content should wash more of the hero (${taller.washStart} vs ${tall.washStart})",
        )
    }

    /** The content wash must never run into the separate wash protecting the status bar. */
    @Test
    fun testTheWashNeverClimbsIntoTheTopScrim() {
        val stops = HeroScrimStops.forContentTop(ABSURDLY_TALL_CONTENT_TOP)

        assertTrue(stops.washStart >= TOP_SCRIM_END, "wash start ${stops.washStart} collided with the top wash")
        assertTrue(stops.washStart < stops.fullStrengthAt, "stops must stay strictly increasing")
    }

    /**
     * Gradient stops have to be strictly increasing, so a hero reporting content taller than itself
     * - a transient during layout - must not produce a brush that throws.
     */
    @Test
    fun testDegenerateMeasurementsStillProduceIncreasingStops() {
        listOf(-1f, 0f, 0.5f, 1f, 2f).forEach { contentTop ->
            val stops = HeroScrimStops.forContentTop(contentTop)

            assertTrue(
                stops.washStart < stops.fullStrengthAt,
                "stops collapsed for contentTop=$contentTop (${stops.washStart}, ${stops.fullStrengthAt})",
            )
            assertTrue(stops.fullStrengthAt < 1f, "the final stop must stay above fullStrengthAt for contentTop=$contentTop")
        }
    }

    /** Before measurement the wash covers the tall case, so no hero opens with an unreadable title. */
    @Test
    fun testTheUnmeasuredDefaultCoversTallContent() {
        assertTrue(
            HeroScrimStops.unmeasured().washStart <= TALL_HERO_CONTENT_TOP,
            "the pre-measurement wash must already start at or above the tallest hero's content",
        )
    }

    private companion object {
        /** Roughly where a movie hero's content begins - title, facts, one button. */
        const val SHORT_HERO_CONTENT_TOP = 0.70f

        /** Roughly where `Reacher`'s content begins, measured on device 2026-08-15. */
        const val TALL_HERO_CONTENT_TOP = 0.50f

        /** Taller than the minimum span, so the content drives the wash rather than the floor. */
        const val VERY_TALL_HERO_CONTENT_TOP = 0.40f

        const val ABSURDLY_TALL_CONTENT_TOP = 0.05f

        /** Rounding slack when comparing two spans for equality. */
        const val SPAN_TOLERANCE = 0.001f

        /**
         * Where the status bar wash has finished. Restated rather than imported: this is the
         * boundary the content wash must not cross, so a test importing it would move with it.
         */
        const val TOP_SCRIM_END = 0.28f
    }
}
