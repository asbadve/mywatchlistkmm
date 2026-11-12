package com.ajinkyabadve.kmmmywatchlist.features.account.viewmodel

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.model.QueuedToggle
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private object AccountMediaListScreenModelTestConstant {
    const val ACCOUNT_ID = 100L
    const val SESSION_ID = "session_abc"
    const val ADDED_MOVIE_ID = 1
    const val ADDED_TV_ID = 2
    const val REMOVED_MOVIE_ID = 3
}

/**
 * Pagination/network-fetch/offline-delete-queue behavior all now lives in
 * `TrackedMediaRepository.pagedFlow`/`TrackedMediaRemoteMediator`, covered against a real database
 * in `desktopTest`'s `TrackedMediaRemoteMediatorTest` - this ScreenModel only wires
 * `category`/`mediaType`/`accountId`/`sessionId` through to the repository's `pagedFlow`, which is
 * all these tests verify (via the fake's call log, not by materializing `PagingData` content -
 * `Flow<PagingData<T>>.asSnapshot()` needs a real `AsyncPagingDataDiffer` pump that isn't worth
 * fighting for a plain wiring check).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AccountMediaListScreenModelTest {
    private val fakeTrackedMediaRepository = FakeTrackedMediaRepository()

    // pendingSync is collected on the model's Main-dispatcher scope.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildModel(
        category: AccountMediaCategory,
        mediaType: SearchMediaType,
    ) = AccountMediaListScreenModel(
        category = category,
        mediaType = mediaType,
        accountId = AccountMediaListScreenModelTestConstant.ACCOUNT_ID,
        sessionId = AccountMediaListScreenModelTestConstant.SESSION_ID,
        trackedMediaRepository = fakeTrackedMediaRepository,
    )

    @Test
    fun testConstructionCallsPagedFlowWithThisCategoryAndMediaType() {
        buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE)

        assertEquals(
            listOf(
                FakeTrackedMediaRepository.PagedFlowCall(
                    category = AccountMediaCategory.FAVORITES,
                    mediaType = SearchMediaType.MOVIE,
                    accountId = AccountMediaListScreenModelTestConstant.ACCOUNT_ID,
                    sessionId = AccountMediaListScreenModelTestConstant.SESSION_ID,
                ),
            ),
            fakeTrackedMediaRepository.pagedFlowCalls,
        )
    }

    @Test
    fun testDifferentMediaTypesCallPagedFlowWithTheirOwnMediaType() {
        buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE)
        buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.TV)

        assertEquals(
            listOf(SearchMediaType.MOVIE, SearchMediaType.TV),
            fakeTrackedMediaRepository.pagedFlowCalls.map { it.mediaType },
        )
    }

    @Test
    fun testDifferentCategoriesCallPagedFlowWithTheirOwnCategory() {
        buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE)
        buildModel(AccountMediaCategory.WATCHLIST, SearchMediaType.MOVIE)

        assertEquals(
            listOf(AccountMediaCategory.FAVORITES, AccountMediaCategory.WATCHLIST),
            fakeTrackedMediaRepository.pagedFlowCalls.map { it.category },
        )
    }

    /** Only this grid's media type, and only additions, get a poster marker; the count covers the whole list. */
    @Test
    fun testPendingSyncMarksThisGridsOfflineAdditionsAndCountsTheWholeList() {
        fakeTrackedMediaRepository.seedQueuedToggles(AccountMediaCategory.FAVORITES, mixedQueue())

        val model = buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE)

        assertEquals(
            PendingSyncUiState(notSyncedIds = setOf(AccountMediaListScreenModelTestConstant.ADDED_MOVIE_ID), waitingCount = 3),
            model.pendingSync.value,
        )
    }

    @Test
    fun testPendingSyncTvGridMarksOnlyTvAdditions() {
        fakeTrackedMediaRepository.seedQueuedToggles(AccountMediaCategory.FAVORITES, mixedQueue())

        val model = buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.TV)

        assertEquals(setOf(AccountMediaListScreenModelTestConstant.ADDED_TV_ID), model.pendingSync.value.notSyncedIds)
    }

    /** Once the queue drains (a send reached TMDB), the markers and the count clear on their own. */
    @Test
    fun testPendingSyncClearsWhenTheQueueDrains() {
        fakeTrackedMediaRepository.seedQueuedToggles(AccountMediaCategory.FAVORITES, mixedQueue())
        val model = buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE)

        fakeTrackedMediaRepository.seedQueuedToggles(AccountMediaCategory.FAVORITES, emptyList())

        assertEquals(PendingSyncUiState(), model.pendingSync.value)
    }

    @Test
    fun testPendingSyncIgnoresTheOtherList() {
        fakeTrackedMediaRepository.seedQueuedToggles(AccountMediaCategory.WATCHLIST, mixedQueue())

        val model = buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE)

        assertEquals(PendingSyncUiState(), model.pendingSync.value)
    }

    private fun mixedQueue() =
        listOf(
            QueuedToggle(AccountMediaListScreenModelTestConstant.ADDED_MOVIE_ID, MediaTypeConstant.MOVIE, isOn = true),
            QueuedToggle(AccountMediaListScreenModelTestConstant.ADDED_TV_ID, MediaTypeConstant.TV, isOn = true),
            QueuedToggle(AccountMediaListScreenModelTestConstant.REMOVED_MOVIE_ID, MediaTypeConstant.MOVIE, isOn = false),
        )
}
