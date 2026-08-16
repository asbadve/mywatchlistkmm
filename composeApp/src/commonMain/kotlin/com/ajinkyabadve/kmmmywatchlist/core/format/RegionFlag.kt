package com.ajinkyabadve.kmmmywatchlist.core.format

private const val REGIONAL_INDICATOR_BASE = 0x1F1E6 // Codepoint for the flag-emoji "A" symbol.

/**
 * ISO 3166-1 alpha-2 -> flag emoji, composed from Unicode Regional Indicator Symbols so it
 * renders with whatever emoji font the device already has - no bundled flag images to fetch or
 * theme. Rendering quality (a colored flag vs. two letters) depends on the platform's emoji font.
 */
fun String.toRegionFlagEmoji(): String {
    if (length != 2) return ""
    val builder = StringBuilder()
    for (char in uppercase()) {
        if (char !in 'A'..'Z') return ""
        builder.appendCodePoint(REGIONAL_INDICATOR_BASE + (char - 'A'))
    }
    return builder.toString()
}
