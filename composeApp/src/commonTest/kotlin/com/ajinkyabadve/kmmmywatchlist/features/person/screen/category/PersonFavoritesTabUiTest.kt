package com.ajinkyabadve.kmmmywatchlist.features.person.screen.category

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FakeFavoritePersonRepository
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PersonFavoritesTabUiTest {
    @Test
    fun testEmptyState_showsWhenNoFavorites() =
        runComposeUiTest {
            setContent {
                PersonFavoritesTab(viewModel = PersonFavoritesScreenModel(FakeFavoritePersonRepository()))
            }

            onNodeWithText("No favorites yet").assertExists()
        }

    /** The local-only caveat is unconditional on this tab, unlike `FollowPersonButton`'s (which
     *  only shows once actually followed) - here it's the tab's whole reason for existing. */
    @Test
    fun testLocalOnlyCaveat_alwaysShown() =
        runComposeUiTest {
            setContent {
                PersonFavoritesTab(viewModel = PersonFavoritesScreenModel(FakeFavoritePersonRepository()))
            }

            onNodeWithText(
                "Favorited people are saved on this device only - they're not tied to your TMDB account and won't appear on your other devices",
            ).assertExists()
        }

    @Test
    fun testFavoritedPerson_rendersAndClickInvokesOnPersonSelected() =
        runComposeUiTest {
            val fakeFavoritePersonRepository =
                FakeFavoritePersonRepository().apply {
                    seedFavorite(personId = 500, name = "Favorited Person")
                }
            var selectedPersonId: Long? = null

            setContent {
                PersonFavoritesTab(
                    viewModel = PersonFavoritesScreenModel(fakeFavoritePersonRepository),
                    onPersonSelected = { selectedPersonId = it },
                )
            }

            onAllNodesWithText("Favorited Person")[0].performClick()

            assertEquals(500L, selectedPersonId)
        }
}
