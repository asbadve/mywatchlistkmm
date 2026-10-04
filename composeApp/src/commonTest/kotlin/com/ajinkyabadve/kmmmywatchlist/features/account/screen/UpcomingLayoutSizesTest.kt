package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpcomingLayoutSizesTest {
    private val compact = UpcomingLayoutSizes.forWindowSize(WindowSize.COMPACT)
    private val medium = UpcomingLayoutSizes.forWindowSize(WindowSize.MEDIUM)
    private val expanded = UpcomingLayoutSizes.forWindowSize(WindowSize.EXPANDED)

    @Test
    fun testCompact_keepsThePhoneSizesAndStacksRows() {
        assertEquals(PHONE_ROW_POSTER_WIDTH, compact.rowPosterWidth)
        assertEquals(PHONE_NEXT_UP_POSTER_WIDTH, compact.nextUpPosterWidth)
        assertTrue(compact.stackedRows)
        assertTrue(compact.rowContentHeight > compact.rowPosterHeight)
    }

    @Test
    fun testPostersGrowWithTheWindowAndWideRowsAreSideBySide() {
        assertTrue(compact.rowPosterWidth < medium.rowPosterWidth && medium.rowPosterWidth < expanded.rowPosterWidth)
        assertTrue(compact.nextUpPosterWidth < medium.nextUpPosterWidth && medium.nextUpPosterWidth < expanded.nextUpPosterWidth)
        assertFalse(medium.stackedRows)
        assertFalse(expanded.stackedRows)
        assertEquals(expanded.rowPosterHeight, expanded.rowContentHeight)
    }

    @Test
    fun testEveryPosterIsTwoByThree() {
        listOf(compact, medium, expanded).forEach { sizes ->
            assertEquals(sizes.rowPosterWidth * POSTER_HEIGHT_RATIO, sizes.rowPosterHeight)
            assertEquals(sizes.nextUpPosterWidth * POSTER_HEIGHT_RATIO, sizes.nextUpPosterHeight)
        }
    }

    private companion object {
        val PHONE_ROW_POSTER_WIDTH = 64.dp
        val PHONE_NEXT_UP_POSTER_WIDTH = 96.dp
        const val POSTER_HEIGHT_RATIO = 1.5f
    }
}
