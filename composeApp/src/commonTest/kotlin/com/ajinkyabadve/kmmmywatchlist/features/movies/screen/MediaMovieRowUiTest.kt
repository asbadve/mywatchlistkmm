package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MediaMovieRowUiTest {
    @Test
    fun testMediaMovieRow_isLoadingState_displaysShimmerAndNoText() =
        runComposeUiTest {
            setContent {
                mediaMovieRow(
                    imageUrl = null,
                    title = "Inception",
                    modifier = Modifier,
                    onClick = {},
                    isLoadingState = true,
                )
            }

            // Verify that in loading state, the shimmer box is displayed with appropriate semantics
            onNodeWithContentDescription("Inception").assertExists()
            // Verify that the title text is not directly visible on the card
            onNodeWithText("Inception").assertDoesNotExist()
        }

    @Test
    fun testMediaMovieRow_imageUrlNull_displaysFallbackAndTriggersClick() =
        runComposeUiTest {
            var clicked = false
            setContent {
                mediaMovieRow(
                    imageUrl = null,
                    title = "Inception",
                    modifier = Modifier,
                    onClick = { clicked = true },
                    isLoadingState = false,
                )
            }

            // The card is announced by its title alone - screen readers add the click hint themselves.
            onNodeWithContentDescription("Inception").assertExists()
            onNodeWithText("Inception").assertExists()
            onNodeWithText("Inception").performClick()
            assertTrue(clicked)
        }

    @Test
    fun testMediaMovieRow_imageUrlNotNull_displaysMovieAndTriggersClick() =
        runComposeUiTest {
            var clicked = false
            setContent {
                mediaMovieRow(
                    imageUrl = "https://example.com/poster.jpg",
                    title = "Inception",
                    modifier = Modifier,
                    onClick = { clicked = true },
                    isLoadingState = false,
                )
            }

            onNodeWithText("Inception").assertExists()
            onNodeWithText("Inception").performClick()
            assertTrue(clicked)
        }
}
