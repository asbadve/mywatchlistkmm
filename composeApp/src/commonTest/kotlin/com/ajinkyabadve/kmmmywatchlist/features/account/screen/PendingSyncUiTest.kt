package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.model.QueuedToggle
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

/**
 * The Favorites/Watchlist grid's "not synced" UI: a marker on each poster added offline, one line
 * above the grid counting every waiting change, and both clearing on their own once the queue
 * drains. The queue itself (what gets queued, sent, kept) is covered by TrackedMediaOutboxTest.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PendingSyncUiTest {
    // The grid's ScreenModel collects pendingSync on its Main-dispatcher scope.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testOnlyTheTitleAddedOfflineGetsTheNotSyncedMarker() =
        runComposeUiTest {
            val fake = fakeWithTwoFavorites()
            fake.seedQueuedToggles(AccountMediaCategory.FAVORITES, listOf(QueuedToggle(OFFLINE_ID, MediaTypeConstant.MOVIE, isOn = true)))
            setContent { tab(fake) }
            waitUntilExactlyOneExists(hasText(SYNCED_TITLE))

            onAllNodesWithTag(NotSyncedBadgeConstant.TAG).assertCountEquals(1)
            onNodeWithTag(NotSyncedBadgeConstant.TAG).assertExists()
            waitUntilExactlyOneExists(hasContentDescription(NOT_SYNCED_LABEL))
        }

    /** A removal has no poster left to mark - it only shows in the line's count. */
    @Test
    fun testTheLineCountsEveryWaitingChangeIncludingRemovals() =
        runComposeUiTest {
            val fake = fakeWithTwoFavorites()
            fake.seedQueuedToggles(
                AccountMediaCategory.FAVORITES,
                listOf(
                    QueuedToggle(OFFLINE_ID, MediaTypeConstant.MOVIE, isOn = true),
                    QueuedToggle(REMOVED_ID, MediaTypeConstant.MOVIE, isOn = false),
                ),
            )
            setContent { tab(fake) }

            waitUntilExactlyOneExists(hasTestTag(PendingSyncLineConstant.TAG))
            waitUntilExactlyOneExists(hasText(TWO_WAITING_TEXT, substring = true))
            onAllNodesWithTag(NotSyncedBadgeConstant.TAG).assertCountEquals(1)
        }

    @Test
    fun testNothingWaitingShowsNoLineAndNoMarkers() =
        runComposeUiTest {
            val fake = fakeWithTwoFavorites()
            setContent { tab(fake) }
            waitUntilExactlyOneExists(hasText(SYNCED_TITLE))

            onNodeWithTag(PendingSyncLineConstant.TAG).assertDoesNotExist()
            onAllNodesWithTag(NotSyncedBadgeConstant.TAG).assertCountEquals(0)
        }

    /** The change reached TMDB (the queue drained): marker and line go away without a refresh. */
    @Test
    fun testMarkerAndLineClearOnceTheQueueDrains() =
        runComposeUiTest {
            val fake = fakeWithTwoFavorites()
            fake.seedQueuedToggles(AccountMediaCategory.FAVORITES, listOf(QueuedToggle(OFFLINE_ID, MediaTypeConstant.MOVIE, isOn = true)))
            setContent { tab(fake) }
            waitUntilExactlyOneExists(hasTestTag(NotSyncedBadgeConstant.TAG))

            fake.seedQueuedToggles(AccountMediaCategory.FAVORITES, emptyList())

            waitUntilDoesNotExist(hasTestTag(NotSyncedBadgeConstant.TAG))
            waitUntilDoesNotExist(hasTestTag(PendingSyncLineConstant.TAG))
        }

    /** A change waiting in the watchlist says nothing on the Favorites tab. */
    @Test
    fun testTheOtherListsQueueDoesNotShowHere() =
        runComposeUiTest {
            val fake = fakeWithTwoFavorites()
            fake.seedQueuedToggles(AccountMediaCategory.WATCHLIST, listOf(QueuedToggle(OFFLINE_ID, MediaTypeConstant.MOVIE, isOn = true)))
            setContent { tab(fake) }
            waitUntilExactlyOneExists(hasText(SYNCED_TITLE))

            onNodeWithTag(PendingSyncLineConstant.TAG).assertDoesNotExist()
            onAllNodesWithTag(NotSyncedBadgeConstant.TAG).assertCountEquals(0)
        }

    @Composable
    private fun tab(fake: FakeTrackedMediaRepository) =
        AccountFavoritesWatchlistTab(
            category = AccountMediaCategory.FAVORITES,
            session = UserSession(sessionId = SESSION_ID, accountId = ACCOUNT_ID, username = USERNAME, name = USERNAME),
            onMovieSelected = {},
            onTvSelected = {},
            trackedMediaRepository = fake,
        )

    private fun fakeWithTwoFavorites() =
        FakeTrackedMediaRepository().apply {
            seedPaged(
                AccountMediaCategory.FAVORITES,
                SearchMediaType.MOVIE,
                listOf(
                    SearchResultItem(id = SYNCED_ID, mediaTypeRaw = MediaTypeConstant.MOVIE, title = SYNCED_TITLE),
                    SearchResultItem(id = OFFLINE_ID, mediaTypeRaw = MediaTypeConstant.MOVIE, title = OFFLINE_TITLE),
                ),
            )
        }

    private companion object {
        const val SESSION_ID = "session"
        const val ACCOUNT_ID = 1L
        const val USERNAME = "user"
        const val SYNCED_ID = 1
        const val SYNCED_TITLE = "Already Synced"
        const val OFFLINE_ID = 2
        const val OFFLINE_TITLE = "Favorited Offline"
        const val REMOVED_ID = 3

        // The contract under test: what screen readers hear and what the line says.
        const val NOT_SYNCED_LABEL = "Not synced yet"
        const val TWO_WAITING_TEXT = "2 changes not synced to TMDB yet"
    }
}
