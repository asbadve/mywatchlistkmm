package com.ajinkyabadve.kmmmywatchlist.core.constant

/** Region codes the app singles out by name rather than taking from the device locale. */
object RegionConstant {
    /**
     * TMDB always publishes a US bucket for release dates and content ratings, so it is the
     * fallback when the viewer's own region has no entry.
     */
    const val US = "US"
}
