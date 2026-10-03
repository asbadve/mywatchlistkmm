package com.ajinkyabadve.kmmmywatchlist.design.calendar

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MonthCalendarCardUiTest {
    @Test
    fun testCard_rendersMonthTitleAndLegendLabels() =
        runComposeUiTest {
            setContent {
                MonthCalendarCard(
                    year = YEAR,
                    monthNumber = MONTH,
                    markedDays = setOf(MARKED_DAY),
                    today = LocalDate(YEAR, MONTH, TODAY_DAY),
                    markedLabel = MARKED_LABEL,
                    todayLabel = TODAY_LABEL,
                )
            }

            onNodeWithText(MONTH_TITLE).assertExists()
            onNodeWithText(MARKED_LABEL).assertExists()
            onNodeWithText(TODAY_LABEL).assertExists()
        }

    private companion object {
        const val YEAR = 2026
        const val MONTH = 10
        const val MARKED_DAY = 4
        const val TODAY_DAY = 3
        const val MONTH_TITLE = "October 2026"
        const val MARKED_LABEL = "Release"
        const val TODAY_LABEL = "Today"
    }
}
