package com.ajinkyabadve.kmmmywatchlist.core.constant

/** Region codes the app singles out by name rather than taking from the device locale. */
object RegionConstant {
    /**
     * TMDB always publishes a US bucket for release dates and content ratings, so it is the
     * fallback when the viewer's own region has no entry.
     */
    const val US = "US"

    const val KEY_SELECTED_REGION = "region_selected_code"
    const val KEY_FALLBACK_REGION = "region_fallback_code"
    const val KEY_REGIONS_CACHE = "region_available_cache"
    const val KEY_REGIONS_TIMESTAMP = "region_available_timestamp"

    /** The available-regions list barely changes, so a week-long cache is plenty fresh. */
    const val REGIONS_CACHE_TTL_MILLIS = 7L * ConfigurationConstants.DAY_IN_MILLIS
}
