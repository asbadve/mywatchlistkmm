package com.ajinkyabadve.kmmmywatchlist.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PageAppendTest {
    @Test
    fun testAddAllNewBy_skipsKeysAlreadyPresentOrRepeatedInThePage() {
        val list = mutableListOf(FIRST, SECOND)

        list.addAllNewBy(listOf(SECOND, THIRD, THIRD)) { it }

        assertEquals(listOf(FIRST, SECOND, THIRD), list)
    }

    private companion object {
        const val FIRST = 1
        const val SECOND = 2
        const val THIRD = 3
    }
}
