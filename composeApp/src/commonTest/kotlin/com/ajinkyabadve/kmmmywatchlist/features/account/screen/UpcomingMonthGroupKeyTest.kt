package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class UpcomingMonthGroupKeyTest {
    @Test
    fun testMonthGroupKey_groupsByYearAndMonthNumber() {
        assertEquals(2026 to 10, monthGroupKey(LocalDate(2026, 10, 4)))
        assertEquals(2026 to 10, monthGroupKey(LocalDate(2026, 10, 31)))
        assertEquals(2027 to 1, monthGroupKey(LocalDate(2027, 1, 1)))
    }
}
