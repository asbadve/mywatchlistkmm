package com.ajinkyabadve.kmmmywatchlist.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormattingTest {
    @Test
    fun testKeepsOneDecimalPlace() {
        assertEquals("7.4", 7.42.toOneDecimalString())
    }

    /**
     * TMDB sends ratings with several decimals; the hero shows one. Truncation rather than rounding
     * is the deliberate choice - a 7.99 shown as "8.0" reads as a different score than the site's.
     */
    @Test
    fun testTruncatesRatherThanRounds() {
        assertEquals("7.9", 7.99.toOneDecimalString())
    }

    /** A whole number still has to render its decimal, or the column of ratings goes ragged. */
    @Test
    fun testWholeNumbersKeepTheirDecimal() {
        assertEquals("8.0", 8.0.toOneDecimalString())
    }

    /** A perfect score is the only two-digit case, and it must not lose its tens digit. */
    @Test
    fun testHandlesTheTopOfTheScale() {
        assertEquals("10.0", 10.0.toOneDecimalString())
    }

    @Test
    fun testHandlesZero() {
        assertEquals("0.0", 0.0.toOneDecimalString())
    }
}
