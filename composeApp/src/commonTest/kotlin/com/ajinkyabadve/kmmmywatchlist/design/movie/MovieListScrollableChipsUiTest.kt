package com.ajinkyabadve.kmmmywatchlist.design.movie

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class MovieListScrollableChipsUiTest {
    private val chips = listOf("Now Playing", "Upcoming", "Popular", "Top Rated")

    @Test
    fun testScrollableChips_rendersAllChipLabels() =
        runComposeUiTest {
            setContent {
                scrollableChips(
                    selectedChip = 0,
                    chipItemList = chips,
                    onClick = {},
                )
            }

            chips.forEach { label -> onNodeWithText(label).assertExists() }
        }

    @Test
    fun testScrollableChips_clickingChip_invokesCallbackWithIndex() =
        runComposeUiTest {
            var clickedIndex: Int? = null
            setContent {
                scrollableChips(
                    selectedChip = 0,
                    chipItemList = chips,
                    onClick = { clickedIndex = it },
                )
            }

            onNodeWithText("Popular").performClick()
            assertEquals(2, clickedIndex)
        }

    @Test
    fun testScrollableChips_isLoadingState_ignoresClicks() =
        runComposeUiTest {
            var clickedIndex: Int? = null
            setContent {
                scrollableChips(
                    selectedChip = 0,
                    chipItemList = chips,
                    onClick = { clickedIndex = it },
                    isLoadingState = true,
                )
            }

            onNodeWithText("Popular").performClick()
            assertNull(clickedIndex)
        }
}
