package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeTrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import kotlin.test.Test
import kotlin.test.assertEquals

private object AccountMediaListScreenModelTestConstant {
    const val ACCOUNT_ID = 100L
    const val SESSION_ID = "session_abc"
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
class AccountMediaListScreenModelTest {
    private val fakeTrackedMediaRepository = FakeTrackedMediaRepository()

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
}
