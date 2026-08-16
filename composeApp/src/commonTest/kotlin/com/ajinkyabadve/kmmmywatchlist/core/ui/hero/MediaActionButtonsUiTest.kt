package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.model.AccountStates
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeAccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private object MediaActionButtonsUiTestConstant {
    const val MOVIE_ID = 42L
    const val SESSION_ID = "session_abc"
    const val ACCOUNT_ID = 100L

    // Values of Res.string.favorite_content_description/watchlist_content_description/
    // action_add_to_list - what MediaActionIconButton sets as each icon's content description.
    const val FAVORITE_CONTENT_DESCRIPTION = "Favorite"
    const val WATCHLIST_CONTENT_DESCRIPTION = "Watchlist"
    const val ADD_TO_LIST_CONTENT_DESCRIPTION = "Add to list"
}

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class MediaActionButtonsUiTest {
    private fun colors() = HeroColors.forTheme(isDark = true, colorScheme = lightColorScheme(), hasSystemStatusBar = false)

    /** Pure component: renders exactly the booleans it's given, nothing pulled from anywhere else. */
    @Test
    fun testRendersFavoriteAndWatchlistFromPlainBooleans() =
        runComposeUiTest {
            var favoriteClicked = false
            var watchlistClicked = false
            setContent {
                MediaActionButtons(
                    isFavorite = true,
                    isInWatchlist = false,
                    showAddToList = true,
                    isLoading = false,
                    colors = colors(),
                    onFavoriteClick = { favoriteClicked = true },
                    onWatchlistClick = { watchlistClicked = true },
                    onAddToListClick = {},
                )
            }

            onNodeWithContentDescription(MediaActionButtonsUiTestConstant.FAVORITE_CONTENT_DESCRIPTION).performClick()
            onNodeWithContentDescription(MediaActionButtonsUiTestConstant.WATCHLIST_CONTENT_DESCRIPTION).performClick()

            assertEquals(true, favoriteClicked)
            assertEquals(true, watchlistClicked)
        }

    /** `showAddToList = false` (the TV case) draws no add-to-list icon at all. */
    @Test
    fun testHidesAddToListIconWhenNotShowable() =
        runComposeUiTest {
            setContent {
                MediaActionButtons(
                    isFavorite = false,
                    isInWatchlist = false,
                    showAddToList = false,
                    isLoading = false,
                    colors = colors(),
                    onFavoriteClick = {},
                    onWatchlistClick = {},
                    onAddToListClick = {},
                )
            }

            onNodeWithContentDescription(MediaActionButtonsUiTestConstant.ADD_TO_LIST_CONTENT_DESCRIPTION).assertDoesNotExist()
        }

    /**
     * While `isLoading`, none of the real icons render - only shimmer placeholders (verified via
     * absence, since the placeholder is decorative and has no content description of its own).
     * This is what stops the row popping from "not favorited" to filled once the real state
     * arrives - see this composable's kdoc.
     */
    @Test
    fun testShowsNoIconsWhileLoading() =
        runComposeUiTest {
            setContent {
                MediaActionButtons(
                    isFavorite = true,
                    isInWatchlist = true,
                    showAddToList = true,
                    isLoading = true,
                    colors = colors(),
                    onFavoriteClick = {},
                    onWatchlistClick = {},
                    onAddToListClick = {},
                )
            }

            onNodeWithContentDescription(MediaActionButtonsUiTestConstant.FAVORITE_CONTENT_DESCRIPTION).assertDoesNotExist()
            onNodeWithContentDescription(MediaActionButtonsUiTestConstant.WATCHLIST_CONTENT_DESCRIPTION).assertDoesNotExist()
            onNodeWithContentDescription(MediaActionButtonsUiTestConstant.ADD_TO_LIST_CONTENT_DESCRIPTION).assertDoesNotExist()
        }
}

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class MediaActionButtonsSectionUiTest {
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeAccountMediaRepository: FakeAccountMediaRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        fakeAuthRepository = FakeAuthRepository()
        fakeAuthRepository.saveSession(
            UserSession(
                sessionId = MediaActionButtonsUiTestConstant.SESSION_ID,
                accountId = MediaActionButtonsUiTestConstant.ACCOUNT_ID,
                username = "jane",
                name = "Jane",
            ),
        )
        fakeAccountMediaRepository = FakeAccountMediaRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun colors() = HeroColors.forTheme(isDark = true, colorScheme = lightColorScheme(), hasSystemStatusBar = false)

    /** Mirrors what the owning ScreenModel does in production before the section ever renders. */
    private fun preloadedMediaActionsState(
        favorite: Boolean,
        watchlist: Boolean,
    ): MediaActionsState {
        fakeAccountMediaRepository.accountStatesResult = Result.success(AccountStates(favorite = favorite, watchlist = watchlist))
        val state =
            MediaActionsState(
                MediaTypeConstant.MOVIE,
                MediaActionButtonsUiTestConstant.MOVIE_ID,
                CoroutineScope(UnconfinedTestDispatcher()),
                fakeAccountMediaRepository,
            )
        state.load(MediaActionButtonsUiTestConstant.SESSION_ID)
        return state
    }

    /**
     * Pre-checked as favorited from `account_states` - tapping it should turn favorite OFF, not on,
     * proving the section reads the `ViewModel`-owned state rather than starting unset.
     */
    @Test
    fun testPreChecksFavoriteFromAccountStates() =
        runComposeUiTest {
            val mediaActionsState = preloadedMediaActionsState(favorite = true, watchlist = false)
            setContent {
                MediaActionButtonsSection(
                    mediaId = MediaActionButtonsUiTestConstant.MOVIE_ID,
                    colors = colors(),
                    showAddToList = false,
                    authRepository = fakeAuthRepository,
                    mediaActionsState = mediaActionsState,
                    listsRepository = FakeListsRepository(),
                )
            }

            onNodeWithContentDescription(MediaActionButtonsUiTestConstant.FAVORITE_CONTENT_DESCRIPTION).performClick()

            assertEquals(listOf(false), fakeAccountMediaRepository.setFavoriteCalls)
        }

    /** Signed out: `MediaActionButtonsSection` renders nothing rather than a disabled row. */
    @Test
    fun testRendersNothingWhenSignedOut() =
        runComposeUiTest {
            val loggedOutAuthRepository = FakeAuthRepository()
            val mediaActionsState = preloadedMediaActionsState(favorite = false, watchlist = false)
            setContent {
                MediaActionButtonsSection(
                    mediaId = MediaActionButtonsUiTestConstant.MOVIE_ID,
                    colors = colors(),
                    showAddToList = false,
                    authRepository = loggedOutAuthRepository,
                    mediaActionsState = mediaActionsState,
                    listsRepository = FakeListsRepository(),
                )
            }

            onNodeWithContentDescription(MediaActionButtonsUiTestConstant.FAVORITE_CONTENT_DESCRIPTION).assertDoesNotExist()
        }
}
