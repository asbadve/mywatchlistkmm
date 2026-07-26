package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MediaTvShowRowUiTest {
    @Test
    fun testMediaTvShowRow_isLoadingState_displaysShimmerAndNoText() =
        runComposeUiTest {
            setContent {
                mediaTvShowRow(
                    imageUrl = null,
                    title = "Breaking Bad",
                    modifier = Modifier,
                    onClick = {},
                    isLoadingState = true,
                )
            }

            onNodeWithContentDescription("Breaking Bad, double tap to activate").assertExists()
            onNodeWithText("Breaking Bad").assertDoesNotExist()
        }

    @Test
    fun testMediaTvShowRow_imageUrlNull_displaysFallbackAndTriggersClick() =
        runComposeUiTest {
            var clicked = false
            setContent {
                mediaTvShowRow(
                    imageUrl = null,
                    title = "Breaking Bad",
                    modifier = Modifier,
                    onClick = { clicked = true },
                    isLoadingState = false,
                )
            }

            onNodeWithText("Breaking Bad").assertExists()
            onNodeWithText("Breaking Bad").performClick()
            assertTrue(clicked)
        }

    @Test
    fun testMediaTvShowRow_imageUrlNotNull_displaysTvShowAndTriggersClick() =
        runComposeUiTest {
            var clicked = false
            setContent {
                mediaTvShowRow(
                    imageUrl = "https://example.com/poster.jpg",
                    title = "Breaking Bad",
                    modifier = Modifier,
                    onClick = { clicked = true },
                    isLoadingState = false,
                )
            }

            onNodeWithText("Breaking Bad").assertExists()
            onNodeWithText("Breaking Bad").performClick()
            assertTrue(clicked)
        }
}
