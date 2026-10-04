package com.ajinkyabadve.kmmmywatchlist.core.constant

/**
 * Compile-time feature switches. Flip a flag to roll a parked feature back in without hunting down
 * every call site; the guarded code stays compiled (and unit-tested) so it doesn't rot while off.
 */
object FeatureFlags {
    /**
     * The Latest Trailers rail on the Trending tab.
     *
     * TMDB has no "list + videos" endpoint, so a source costs ~11 requests (its list, then one
     * videos call per title). Parked 2026-08-04 because that burst starved the rest of the tab on a
     * slow connection; re-enabled 2026-10-04 once `TrendingScreenTabViewModel` made it cheap: the
     * fetch waits for the trending rows' first load, runs at most 3 videos calls at once, shows
     * cards as they arrive, and caches each source on the device for 12 hours
     * (`TrailerCacheRepository`). Turn off again here if it misbehaves; a server-side aggregation
     * (future_features_checklist.md items 1 and 18) would replace the per-title calls entirely.
     */
    const val TRENDING_TRAILERS_ENABLED = true
}
