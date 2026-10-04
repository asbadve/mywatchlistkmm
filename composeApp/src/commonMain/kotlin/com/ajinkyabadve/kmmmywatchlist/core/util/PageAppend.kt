package com.ajinkyabadve.kmmmywatchlist.core.util

/**
 * Appends a fetched page, skipping items whose [key] is already in the list (or repeated within
 * the page). TMDB's paged endpoints - discover above all - can return the same title on two pages
 * when its ranking shifts between requests; appended as-is, that duplicates a poster and crashes a
 * lazy grid keyed by id ("Key ... was already used").
 */
internal fun <T, K> MutableList<T>.addAllNewBy(
    page: List<T>,
    key: (T) -> K,
) {
    val seen = mapTo(HashSet(size + page.size)) { key(it) }
    // Filter first, then one addAll: on a SnapshotStateList that's a single state change instead
    // of one per item.
    addAll(page.filter { seen.add(key(it)) })
}
