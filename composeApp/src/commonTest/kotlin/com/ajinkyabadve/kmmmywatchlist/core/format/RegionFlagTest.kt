package com.ajinkyabadve.kmmmywatchlist.core.format

import kotlin.test.Test
import kotlin.test.assertEquals

class RegionFlagTest {
    @Test
    fun testTwoLetterCodeProducesFlagEmoji() {
        assertEquals("🇺🇸", "US".toRegionFlagEmoji())
        assertEquals("🇮🇳", "IN".toRegionFlagEmoji())
    }

    @Test
    fun testLowercaseCodeProducesTheSameFlagAsUppercase() {
        assertEquals("US".toRegionFlagEmoji(), "us".toRegionFlagEmoji())
    }

    @Test
    fun testWrongLengthReturnsEmptyString() {
        assertEquals("", "USA".toRegionFlagEmoji())
        assertEquals("", "U".toRegionFlagEmoji())
        assertEquals("", "".toRegionFlagEmoji())
    }

    @Test
    fun testNonLetterCharactersReturnEmptyString() {
        assertEquals("", "U1".toRegionFlagEmoji())
    }
}
