package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    /**
     * Reproduces the staleness bug directly: `viewModel(key = ...)` returns the *same*
     * `AccountMediaListScreenModel` across a tab switch-away-and-back (its ViewModelStore outlives
     * the composable being removed from composition), so a title favorited elsewhere while this
     * tab sat cached used to stay invisible here until a manual pull-to-refresh. The
     * `LaunchedEffect(screenModel) { screenModel.refresh() }` this test guards re-syncs on every
     * remount instead.
     */
    @Test
    fun testRefreshesOnRemountAfterBeingNavigatedAwayAndBack() =
        runComposeUiTest {
            val fake =
                FakeAccountMediaRepository().apply {
                    favoriteMoviesResult =
                        Result.success(
                            SearchPageResult(page = 1, list = listOf(SearchResultItem(id = 1, title = OLD_TITLE)), totalPages = 1),
                        )
                }
            val session = testSession()
            var tabIsMounted by mutableStateOf(true)
            setContent {
                if (tabIsMounted) {
                    AccountFavoritesWatchlistTab(
                        category = AccountMediaCategory.FAVORITES,
                        session = session,
                        onMovieSelected = {},
                        onTvSelected = {},
                        accountMediaRepository = fake,
                    )
                }
            }
            onNodeWithText(OLD_TITLE).assertExists()

            // A different title gets favorited elsewhere (e.g. from a detail screen) while this
            // tab's ScreenModel instance stays alive in the background, uninvolved.
            fake.favoriteMoviesResult =
                Result.success(SearchPageResult(page = 1, list = listOf(SearchResultItem(id = 2, title = NEW_TITLE)), totalPages = 1))

            // Simulates the user switching to another top-level tab and back: this composable is
            // disposed and recomposed fresh, but the cached ScreenModel it fetches via
            // `viewModel(key = ...)` is the same instance as before.
            // Two writes with a `waitForIdle()` between them, not back-to-back: without letting
            // Compose actually process the `false` value first, the snapshot writes collapse into
            // a single recomposition that never disposes the composable at all, so the effect
            // guarded here would never re-fire even if the production fix were deleted.
            tabIsMounted = false
            waitForIdle()
            tabIsMounted = true
            waitForIdle()

            onNodeWithText(NEW_TITLE).assertExists()
        }

    private companion object {
        const val OLD_TITLE = "Old Favorite"
        const val NEW_TITLE = "New Favorite"
        const val MOVIE_ID = 11
        const val MOVIE_TITLE = "Star Wars"
    }
}
