package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeAccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AccountFavoritesWatchlistTabUiTest {
    private fun testSession() = UserSession(sessionId = "session", accountId = 1L, username = "u", name = "n")

    private fun favoriteMoviesPage() =
        SearchPageResult(page = 1, list = listOf(SearchResultItem(id = MOVIE_ID, title = MOVIE_TITLE)), totalPages = 1)

    @Test
    fun testTappingAFavoriteMovieNavigatesToItsDetail() =
        runComposeUiTest {
            val fake = FakeAccountMediaRepository().apply { favoriteMoviesResult = Result.success(favoriteMoviesPage()) }
            var selectedMovieId: Long? = null
            setContent {
                AccountFavoritesWatchlistTab(
                    category = AccountMediaCategory.FAVORITES,
                    session = testSession(),
                    onMovieSelected = { selectedMovieId = it },
                    onTvSelected = {},
                    accountMediaRepository = fake,
                )
            }

            onNodeWithText(MOVIE_TITLE).performClick()

            assertEquals(MOVIE_ID.toLong(), selectedMovieId)
        }

    /**
     * The pull-to-refresh gesture itself is threshold/velocity-based and not reliably reproducible
     * headless (a `swipeDown()` short of that threshold is a no-op no matter how it's tuned) - this
     * only proves the grid is wrapped in the drag-to-refresh surface at all. What `onRefresh`
     * actually does - discard pagination progress and re-fetch page one - is covered directly by
     * `AccountMediaListScreenModelTest.testRefreshReplacesItemsFromPageOneRatherThanPaginating`.
     */
    @Test
    fun testGridIsWrappedInPullToRefresh() =
        runComposeUiTest {
            val fake = FakeAccountMediaRepository().apply { favoriteMoviesResult = Result.success(favoriteMoviesPage()) }
            setContent {
                AccountFavoritesWatchlistTab(
                    category = AccountMediaCategory.FAVORITES,
                    session = testSession(),
                    onMovieSelected = {},
                    onTvSelected = {},
                    accountMediaRepository = fake,
                )
            }

            onNodeWithTag(AccountFavoritesWatchlistTabConstant.PULL_TO_REFRESH_TAG).assertExists()
        }

    private companion object {
        const val MOVIE_ID = 11
        const val MOVIE_TITLE = "Star Wars"
    }
}
