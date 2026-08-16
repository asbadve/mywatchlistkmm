package com.ajinkyabadve.kmmmywatchlist.core.format

private const val REGIONAL_INDICATOR_BASE = 0x1F1E6 // Codepoint for the flag-emoji "A" symbol.
private const val SURROGATE_PAIR_OFFSET = 0x10000
private const val HIGH_SURROGATE_BASE = 0xD800
private const val LOW_SURROGATE_BASE = 0xDC00
private const val LOW_SURROGATE_MASK = 0x3FF

/**
 * ISO 3166-1 alpha-2 -> flag emoji, composed from Unicode Regional Indicator Symbols so it
 * renders with whatever emoji font the device already has - no bundled flag images to fetch or
 * theme. Rendering quality (a colored flag vs. two letters) depends on the platform's emoji font.
 *
 * Regional indicator symbols sit above U+FFFF, so each one needs a UTF-16 surrogate pair - built
 * by hand here rather than via `StringBuilder.appendCodePoint`, which is `internal` on the
 * Kotlin/Native stdlib (compiles on JVM/JS, fails on iOS) even though it's a public common API.
 */
fun String.toRegionFlagEmoji(): String {
    if (length != 2) return ""
    val builder = StringBuilder()
    for (char in uppercase()) {
        if (char !in 'A'..'Z') return ""
        val offset = REGIONAL_INDICATOR_BASE + (char - 'A') - SURROGATE_PAIR_OFFSET
        builder.append(((offset shr 10) + HIGH_SURROGATE_BASE).toChar())
        builder.append(((offset and LOW_SURROGATE_MASK) + LOW_SURROGATE_BASE).toChar())
    }
    return builder.toString()
}
