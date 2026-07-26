package com.ajinkyabadve.kmmmywatchlist.features.person.screen

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MediaPersonRowUiTest {
    @Test
    fun testMediaPersonRow_isLoadingState_displaysShimmerAndNoText() =
        runComposeUiTest {
            setContent {
                mediaPersonRow(
                    imageUrl = null,
                    name = "Bryan Cranston",
                    modifier = Modifier,
                    onClick = {},
                    isLoadingState = true,
                )
            }

            onNodeWithContentDescription("Bryan Cranston, double tap to activate").assertExists()
            onNodeWithText("Bryan Cranston").assertDoesNotExist()
        }

    @Test
    fun testMediaPersonRow_imageUrlNull_displaysFallbackAndTriggersClick() =
        runComposeUiTest {
            var clicked = false
            setContent {
                mediaPersonRow(
                    imageUrl = null,
                    name = "Bryan Cranston",
                    modifier = Modifier,
                    onClick = { clicked = true },
                    isLoadingState = false,
                )
            }

            onNodeWithText("Bryan Cranston").assertExists()
            onNodeWithText("Bryan Cranston").performClick()
            assertTrue(clicked)
        }

    @Test
    fun testMediaPersonRow_imageUrlNotNull_displaysPersonAndTriggersClick() =
        runComposeUiTest {
            var clicked = false
            setContent {
                mediaPersonRow(
                    imageUrl = "https://example.com/profile.jpg",
                    name = "Bryan Cranston",
                    modifier = Modifier,
                    onClick = { clicked = true },
                    isLoadingState = false,
                )
            }

            onNodeWithText("Bryan Cranston").assertExists()
            onNodeWithText("Bryan Cranston").performClick()
            assertTrue(clicked)
        }
}
