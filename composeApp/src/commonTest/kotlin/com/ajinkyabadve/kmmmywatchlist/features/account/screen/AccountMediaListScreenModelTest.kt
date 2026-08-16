package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeAccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeAuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private object AccountMediaListScreenModelTestConstant {
    const val ACCOUNT_ID = 100L
    const val SESSION_ID = "session_abc"
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountMediaListScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeAccountMediaRepository = FakeAccountMediaRepository()
    private val fakeAuthRepository = FakeAuthRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
        accountMediaRepository = fakeAccountMediaRepository,
        authRepository = fakeAuthRepository,
    )

    @Test
    fun testFavoritesMoviesLoadsFromFavoriteMoviesEndpoint() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.favoriteMoviesResult =
                Result.success(
                    SearchPageResult(page = 1, list = listOf(SearchResultItem(id = 1, title = "Movie A")), totalPages = 1),
                )

            val viewModel = buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE)

            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
            assertEquals(1, viewModel.items.size)
            assertEquals("Movie A", viewModel.items.first().displayTitle)
        }

    @Test
    fun testWatchlistTvLoadsFromWatchlistTvEndpoint() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.watchlistTvResult =
                Result.success(
                    SearchPageResult(page = 1, list = listOf(SearchResultItem(id = 2, name = "Show B")), totalPages = 1),
                )

            val viewModel = buildModel(AccountMediaCategory.WATCHLIST, SearchMediaType.TV)

            assertEquals(1, viewModel.items.size)
            assertEquals("Show B", viewModel.items.first().displayTitle)
        }

    @Test
    fun testCanPaginateWhenMorePagesExist() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.favoriteMoviesResult =
                Result.success(
                    SearchPageResult(page = 1, list = listOf(SearchResultItem(id = 1, title = "Movie A")), totalPages = 2),
                )

            val viewModel = buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE)

            assertEquals(ListState.IDLE, viewModel.listState)
        }

    @Test
    fun testNetworkErrorSetsNetworkErrorState() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.favoriteMoviesResult = Result.failure(IOException("Mock network failure"))

            val viewModel = buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE)

            assertEquals(ListState.NETWORK_ERROR, viewModel.listState)
            assertTrue(viewModel.items.isEmpty())
        }

    /** Pull-to-refresh discards pagination progress and re-fetches page one, not the next page. */
    @Test
    fun testRefreshReplacesItemsFromPageOneRatherThanPaginating() =
        runTest(testDispatcher) {
            fakeAccountMediaRepository.favoriteMoviesResult =
                Result.success(
                    SearchPageResult(page = 1, list = listOf(SearchResultItem(id = 1, title = "Movie A")), totalPages = 2),
                )
            val viewModel = buildModel(AccountMediaCategory.FAVORITES, SearchMediaType.MOVIE)
            assertEquals(ListState.IDLE, viewModel.listState)

            fakeAccountMediaRepository.favoriteMoviesResult =
                Result.success(
                    SearchPageResult(page = 1, list = listOf(SearchResultItem(id = 9, title = "Movie Z")), totalPages = 1),
                )
            viewModel.refresh()

            assertEquals(1, viewModel.items.size)
            assertEquals("Movie Z", viewModel.items.first().displayTitle)
            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
        }
}
