package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeFavoriteCollectionRepository
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FavoriteCollectionsTabUiTest {
    @Test
    fun testEmptyState_showsWhenNoFavorites() =
        runComposeUiTest {
            setContent {
                FavoriteCollectionsTab(viewModel = FavoriteCollectionsScreenModel(FakeFavoriteCollectionRepository()))
            }

            onNodeWithText("No collections yet").assertExists()
        }

    /** The local-only caveat is unconditional on this tab, unlike `FollowCollectionButton`'s
     *  (which only shows once actually followed) - here it's the tab's whole reason for existing. */
    @Test
    fun testLocalOnlyCaveat_alwaysShown() =
        runComposeUiTest {
            setContent {
                FavoriteCollectionsTab(viewModel = FavoriteCollectionsScreenModel(FakeFavoriteCollectionRepository()))
            }

            onNodeWithText(
                "Followed collections are saved on this device only - they're not tied to your TMDB account and won't appear on your other devices",
            ).assertExists()
        }

    @Test
    fun testFollowedCollection_rendersAndClickInvokesOnCollectionSelected() =
        runComposeUiTest {
            val fakeFavoriteCollectionRepository =
                FakeFavoriteCollectionRepository().apply {
                    seedFavorite(collectionId = 500, name = "Followed Collection")
                }
            var selectedCollectionId: Long? = null

            setContent {
                FavoriteCollectionsTab(
                    viewModel = FavoriteCollectionsScreenModel(fakeFavoriteCollectionRepository),
                    onCollectionSelected = { selectedCollectionId = it },
                )
            }

            onAllNodesWithText("Followed Collection")[0].performClick()

            assertEquals(500L, selectedCollectionId)
        }
}
