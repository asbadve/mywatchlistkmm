package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

private object FollowCollectionButtonUiTestConstant {
    // Values of Res.string.follow_collection_label/following_collection_label.
    const val FOLLOW_LABEL = "Follow"
    const val FOLLOWING_LABEL = "Following"

    // Value of Res.string.follow_collection_local_only_caveat.
    const val LOCAL_ONLY_CAVEAT = "Saved on this device only — not tied to your TMDB account, so it won't appear on your other devices"
}

@OptIn(ExperimentalTestApi::class)
class FollowCollectionButtonUiTest {
    @Test
    fun testRendersFollowLabelWhenNotFollowing() =
        runComposeUiTest {
            setContent {
                FollowCollectionButton(isFollowing = false, onToggleClick = {})
            }

            onNodeWithText(FollowCollectionButtonUiTestConstant.FOLLOW_LABEL).assertExists()
        }

    @Test
    fun testRendersFollowingLabelWhenFollowing() =
        runComposeUiTest {
            setContent {
                FollowCollectionButton(isFollowing = true, onToggleClick = {})
            }

            onNodeWithText(FollowCollectionButtonUiTestConstant.FOLLOWING_LABEL).assertExists()
        }

    /** A never-followed collection's page shouldn't get a disclaimer about a state that isn't true
     *  yet - the caveat only shows once actually followed (see the other half of this pair below). */
    @Test
    fun testHidesLocalOnlyCaveatWhenNotFollowing() =
        runComposeUiTest {
            setContent {
                FollowCollectionButton(isFollowing = false, onToggleClick = {})
            }

            onNodeWithText(FollowCollectionButtonUiTestConstant.LOCAL_ONLY_CAVEAT).assertDoesNotExist()
        }

    @Test
    fun testShowsLocalOnlyCaveatWhenFollowing() =
        runComposeUiTest {
            setContent {
                FollowCollectionButton(isFollowing = true, onToggleClick = {})
            }

            onNodeWithText(FollowCollectionButtonUiTestConstant.LOCAL_ONLY_CAVEAT).assertExists()
        }

    @Test
    fun testClickInvokesCallback() =
        runComposeUiTest {
            var clicked = false
            setContent {
                FollowCollectionButton(isFollowing = false, onToggleClick = { clicked = true })
            }

            onNodeWithText(FollowCollectionButtonUiTestConstant.FOLLOW_LABEL).performClick()

            assertTrue(clicked)
        }
}
