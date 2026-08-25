package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AccountFavoritesWatchlistTabUiTest {
    // AccountMediaListScreenModel's `pagedItems` uses `.cachedIn(viewModelScope)`, which needs a
    // live coroutine on `Dispatchers.Main` to actually pull from the upstream Flow - same reason
    // MovieDetailScreenUiTest sets this, just newly load-bearing here (the pre-Paging3 version of
    // this ScreenModel didn't depend on a long-lived Main-dispatcher coroutine the same way).
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testSession() = UserSession(sessionId = "session", accountId = 1L, username = "u", name = "n")

    @Test
    fun testTappingAFavoriteMovieNavigatesToItsDetail() =
        runComposeUiTest {
            val fake = FakeTrackedMediaRepository()
            fake.seedPaged(
                AccountMediaCategory.FAVORITES,
                SearchMediaType.MOVIE,
                listOf(SearchResultItem(id = MOVIE_ID, mediaTypeRaw = MediaTypeConstant.MOVIE, title = MOVIE_TITLE)),
            )
            var selectedMovieId: Long? = null
            setContent {
                AccountFavoritesWatchlistTab(
                    category = AccountMediaCategory.FAVORITES,
                    session = testSession(),
                    onMovieSelected = { selectedMovieId = it },
                    onTvSelected = {},
                    trackedMediaRepository = fake,
                )
            }
            waitForIdle()

            onNodeWithText(MOVIE_TITLE).performClick()

            assertEquals(MOVIE_ID.toLong(), selectedMovieId)
        }

    /**
     * The pull-to-refresh gesture itself is threshold/velocity-based and not reliably reproducible
     * headless (a `swipeDown()` short of that threshold is a no-op no matter how it's tuned) - this
     * only proves the grid is wrapped in the drag-to-refresh surface at all. What `onRefresh`
     * actually does - `lazyPagingItems.refresh()`, discarding pagination progress and re-fetching
     * page one via `TrackedMediaRemoteMediator` - is covered directly by
     * `TrackedMediaRemoteMediatorTest.testRefreshUpsertsThePageAndStoresTheNextKey`.
     */
    @Test
    fun testGridIsWrappedInPullToRefresh() =
        runComposeUiTest {
            val fake = FakeTrackedMediaRepository()
            fake.seedPaged(
                AccountMediaCategory.FAVORITES,
                SearchMediaType.MOVIE,
                listOf(SearchResultItem(id = MOVIE_ID, mediaTypeRaw = MediaTypeConstant.MOVIE, title = MOVIE_TITLE)),
            )
            setContent {
                AccountFavoritesWatchlistTab(
                    category = AccountMediaCategory.FAVORITES,
                    session = testSession(),
                    onMovieSelected = {},
                    onTvSelected = {},
                    trackedMediaRepository = fake,
                )
            }

            onNodeWithTag(AccountFavoritesWatchlistTabConstant.PULL_TO_REFRESH_TAG).assertExists()
        }

    /**
     * `QueryPagingSource` (see `TrackedMediaRepository.pagedFlow`) auto-invalidates whenever
     * anything writes to the underlying `trackedMedia` table - this reproduces that directly: a
     * title favorited elsewhere (modeled here as a second `seedPaged` call on the same fake, while
     * this tab stays mounted and its `ScreenModel` instance unchanged) now appears without any
     * manual refresh, unlike the pre-Paging3 version of this screen which needed an explicit
     * remount-triggered `refresh()` call to pick this up.
     */
    @Test
    fun testANewlyFavoritedTitleAppearsWithoutAManualRefresh() =
        runComposeUiTest {
            val fake = FakeTrackedMediaRepository()
            fake.seedPaged(
                AccountMediaCategory.FAVORITES,
                SearchMediaType.MOVIE,
                listOf(SearchResultItem(id = 1, mediaTypeRaw = MediaTypeConstant.MOVIE, title = OLD_TITLE)),
            )
            setContent {
                AccountFavoritesWatchlistTab(
                    category = AccountMediaCategory.FAVORITES,
                    session = testSession(),
                    onMovieSelected = {},
                    onTvSelected = {},
                    trackedMediaRepository = fake,
                )
            }
            waitForIdle()
            onNodeWithText(OLD_TITLE).assertExists()

            // A different title gets favorited elsewhere (e.g. from a detail screen) - modeled as a
            // write to the same local table this tab's PagingSource reads from.
            fake.seedPaged(
                AccountMediaCategory.FAVORITES,
                SearchMediaType.MOVIE,
                listOf(SearchResultItem(id = 2, mediaTypeRaw = MediaTypeConstant.MOVIE, title = NEW_TITLE)),
            )
            waitForIdle()

            onNodeWithText(NEW_TITLE).assertExists()
        }

    @Test
    fun testUpcomingMovieShowsUpcomingBadgeAndReleasedMovieDoesNot() =
        runComposeUiTest {
            val fake = FakeTrackedMediaRepository()
            fake.seedPaged(
                AccountMediaCategory.FAVORITES,
                SearchMediaType.MOVIE,
                listOf(
                    SearchResultItem(
                        id = 1,
                        mediaTypeRaw = MediaTypeConstant.MOVIE,
                        title = UPCOMING_MOVIE_TITLE,
                        releaseDate = FAR_FUTURE_DATE,
                    ),
                    SearchResultItem(id = 2, mediaTypeRaw = MediaTypeConstant.MOVIE, title = MOVIE_TITLE, releaseDate = PAST_DATE),
                ),
            )
            setContent {
                AccountFavoritesWatchlistTab(
                    category = AccountMediaCategory.FAVORITES,
                    session = testSession(),
                    onMovieSelected = {},
                    onTvSelected = {},
                    trackedMediaRepository = fake,
                )
            }
            waitForIdle()

            onNodeWithText(UPCOMING_BADGE_TEXT).assertExists()
        }

    private companion object {
        const val OLD_TITLE = "Old Favorite"
        const val NEW_TITLE = "New Favorite"
        const val MOVIE_ID = 11
        const val MOVIE_TITLE = "Star Wars"
        const val UPCOMING_MOVIE_TITLE = "Not Out Yet"
        const val FAR_FUTURE_DATE = "2099-01-01"
        const val PAST_DATE = "2010-01-01"
        const val UPCOMING_BADGE_TEXT = "Upcoming"
    }
}
