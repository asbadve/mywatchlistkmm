package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TITLE = "Some Detail Title"

// Value of Res.string.back_content_description - every detail screen now shares this one
// affordance, where they previously disagreed between "Back" and "Close".
private const val BACK = "Back"

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class DetailTopBarUiTest {
    @Test
    fun testSolidBarShowsTitleAndBackButton() =
        runComposeUiTest {
            setContent { DetailTopBar(title = TITLE, onBackClicked = {}) }

            onNodeWithText(TITLE).assertExists()
            onNodeWithContentDescription(BACK).assertExists()
        }

    @Test
    fun testBackButtonInvokesCallback() =
        runComposeUiTest {
            var backCount = 0
            setContent { DetailTopBar(title = TITLE, onBackClicked = { backCount++ }) }

            onNodeWithContentDescription(BACK).performClick()

            assertEquals(1, backCount)
        }

    /**
     * Over a backdrop the image carries the context, so the title stays hidden until the caller
     * reports the list has scrolled past it - but the way back must never disappear with it.
     */
    @Test
    fun testTitleIsHiddenWhileTheBarFloatsOverTheHeroImage() =
        runComposeUiTest {
            setContent {
                DetailTopBar(title = TITLE, onBackClicked = {}, isScrolledPastHero = false)
            }

            onNodeWithText(TITLE).assertDoesNotExist()
            onNodeWithContentDescription(BACK).assertExists()
        }

    @Test
    fun testTitleAppearsOnceScrolledPastTheHeroImage() =
        runComposeUiTest {
            setContent {
                DetailTopBar(title = TITLE, onBackClicked = {}, isScrolledPastHero = true)
            }

            onNodeWithText(TITLE).assertExists()
        }

    /**
     * Over the hero the icon has no bar behind it, only the hero's own scrim - so it takes the
     * hero's colours rather than a hardcoded white, which would vanish once that scrim went light.
     */
    @Test
    fun testBackButtonStaysUsableOverTheHeroInLightTheme() =
        runComposeUiTest {
            var backCount = 0
            setContent {
                AppTheme(useDarkTheme = false) {
                    DetailTopBar(title = TITLE, onBackClicked = { backCount++ }, isScrolledPastHero = false)
                }
            }

            onNodeWithContentDescription(BACK).assertIsDisplayed()
            onNodeWithContentDescription(BACK).performClick()

            assertEquals(1, backCount)
        }

    /** EpisodeList drops the back button when it is shown as the detail pane of a split layout. */
    @Test
    fun testBackButtonCanBeSuppressed() =
        runComposeUiTest {
            setContent {
                DetailTopBar(title = TITLE, onBackClicked = {}, showBackButton = false)
            }

            onAllNodesWithContentDescription(BACK).assertCountEquals(0)
            onNodeWithText(TITLE).assertExists()
        }
}
