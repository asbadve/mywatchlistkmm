package com.ajinkyabadve.kmmmywatchlist.core.constant

/**
 * Compile-time feature switches. Flip a flag to roll a parked feature back in without hunting down
 * every call site; the guarded code stays compiled (and unit-tested) so it doesn't rot while off.
 */
object FeatureFlags {
    /**
     * The Latest Trailers rail on the Trending tab.
     *
     * Parked (off) because TMDB has no "list + videos" endpoint: building the rail means fetching a
     * source list and then one videos call per title (up to 10), i.e. a burst of ~11 requests that
     * competes with the rest of the tab on a slow connection. Re-enable once a single-request
     * trailer feed (or a server-side aggregation) is available.
     */
    const val TRENDING_TRAILERS_ENABLED = false
}
