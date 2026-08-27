package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

private object FollowPersonButtonUiTestConstant {
    // Values of Res.string.follow_person_label/following_person_label.
    const val FOLLOW_LABEL = "Follow"
    const val FOLLOWING_LABEL = "Following"

    // Value of Res.string.follow_person_local_only_caveat.
    const val LOCAL_ONLY_CAVEAT = "Saved on this device only — not tied to your TMDB account, so it won't appear on your other devices"
}

@OptIn(ExperimentalTestApi::class)
class FollowPersonButtonUiTest {
    @Test
    fun testRendersFollowLabelWhenNotFollowing() =
        runComposeUiTest {
            setContent {
                FollowPersonButton(isFollowing = false, onToggleClick = {})
            }

            onNodeWithText(FollowPersonButtonUiTestConstant.FOLLOW_LABEL).assertExists()
        }

    @Test
    fun testRendersFollowingLabelWhenFollowing() =
        runComposeUiTest {
            setContent {
                FollowPersonButton(isFollowing = true, onToggleClick = {})
            }

            onNodeWithText(FollowPersonButtonUiTestConstant.FOLLOWING_LABEL).assertExists()
        }

    /** A never-followed person's page shouldn't get a disclaimer about a state that isn't true
     *  yet - the caveat only shows once actually followed (see the other half of this pair below). */
    @Test
    fun testHidesLocalOnlyCaveatWhenNotFollowing() =
        runComposeUiTest {
            setContent {
                FollowPersonButton(isFollowing = false, onToggleClick = {})
            }

            onNodeWithText(FollowPersonButtonUiTestConstant.LOCAL_ONLY_CAVEAT).assertDoesNotExist()
        }

    @Test
    fun testShowsLocalOnlyCaveatWhenFollowing() =
        runComposeUiTest {
            setContent {
                FollowPersonButton(isFollowing = true, onToggleClick = {})
            }

            onNodeWithText(FollowPersonButtonUiTestConstant.LOCAL_ONLY_CAVEAT).assertExists()
        }

    @Test
    fun testClickInvokesCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                FollowPersonButton(isFollowing = false, onToggleClick = { clicked = true })
            }

            onNodeWithText(FollowPersonButtonUiTestConstant.FOLLOW_LABEL).performClick()

            assertTrue(clicked)
        }
}
