package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

/**
 * Values shared by the movie and TV heroes, which are the same design with different facts in it.
 *
 * These were declared identically in both files before; keeping one copy is what stops the two
 * heroes drifting apart the next time one of them is tuned.
 */
object HeroConstant {
    /** Backdrops are requested at this width and cropped, so both heroes fetch the same size. */
    const val BACKDROP_TARGET_WIDTH_DP = 500

    /** Width-to-height of the hero. Tall enough to hold title, facts and buttons over the art. */
    const val HERO_ASPECT_RATIO = 4 / 5f

    /** Beyond two, provider chips wrap and start competing with the primary button. */
    const val MAX_PROVIDER_CHIPS = 2

    /** Between facts in the meta row - wider than a plain interpunct so the row breathes. */
    const val META_SEPARATOR = "  ·  "
}
