package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CurrentSeasonSectionUiTest {
    @Test
    fun testLongOverview_expandsAndCollapses_withoutOpeningTheSeason() =
        runComposeUiTest {
            var seasonClicks = 0
            setContent {
                Box(modifier = Modifier.width(PHONE_WIDTH_DP.dp)) {
                    CurrentSeasonSection(season = season(LONG_OVERVIEW), onSeasonClick = { seasonClicks++ })
                }
            }

            onNodeWithText(READ_MORE).performClick()
            onNodeWithText(READ_LESS).assertExists()
            onNodeWithText(READ_LESS).performClick()
            onNodeWithText(READ_MORE).assertExists()

            // The toggle sits inside the clickable season row; its taps must not open the season.
            assertEquals(0, seasonClicks)
        }

    @Test
    fun testTappingTheExpandedOverview_collapsesIt_withoutOpeningTheSeason() =
        runComposeUiTest {
            var seasonClicks = 0
            setContent {
                Box(modifier = Modifier.width(PHONE_WIDTH_DP.dp)) {
                    CurrentSeasonSection(season = season(LONG_OVERVIEW), onSeasonClick = { seasonClicks++ })
                }
            }

            onNodeWithText(READ_MORE).performClick()
            onNodeWithText(LONG_OVERVIEW).performClick()

            onNodeWithText(READ_MORE).assertExists()
            assertEquals(0, seasonClicks)
        }

    @Test
    fun testTappingTheCollapsedOverview_stillOpensTheSeason() =
        runComposeUiTest {
            var seasonClicks = 0
            setContent {
                Box(modifier = Modifier.width(PHONE_WIDTH_DP.dp)) {
                    CurrentSeasonSection(season = season(LONG_OVERVIEW), onSeasonClick = { seasonClicks++ })
                }
            }

            onNodeWithText(LONG_OVERVIEW).performClick()

            assertEquals(1, seasonClicks)
        }

    @Test
    fun testShortOverview_showsNoToggle() =
        runComposeUiTest {
            setContent {
                Box(modifier = Modifier.width(PHONE_WIDTH_DP.dp)) { CurrentSeasonSection(season = season(SHORT_OVERVIEW)) }
            }

            onNodeWithText(SHORT_OVERVIEW).assertExists()
            onNodeWithText(READ_MORE).assertDoesNotExist()
        }

    @Test
    fun testEmptyOverview_showsTheFallbackText() =
        runComposeUiTest {
            setContent { CurrentSeasonSection(season = season(overview = "")) }

            onNodeWithText(NO_OVERVIEW).assertExists()
        }

    @Test
    fun testTappingTheSeasonName_stillOpensTheSeason() =
        runComposeUiTest {
            var seasonClicks = 0
            setContent { CurrentSeasonSection(season = season(SHORT_OVERVIEW), onSeasonClick = { seasonClicks++ }) }

            onNodeWithText(SEASON_NAME).performClick()

            assertEquals(1, seasonClicks)
        }

    private companion object {
        const val PHONE_WIDTH_DP = 360
        const val SEASON_NAME = "Season 2"
        const val READ_MORE = "Read more"
        const val READ_LESS = "Read less"
        const val NO_OVERVIEW = "No overview available for this season."
        const val SHORT_OVERVIEW = "Short."
        val LONG_OVERVIEW = "The crew faces a long journey across the frozen north. ".repeat(20)

        fun season(overview: String) = TvSeasonDetail(id = 1, seasonNumber = 2, name = SEASON_NAME, overview = overview)
    }
}
