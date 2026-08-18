package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeDiscoverFilterRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeDiscoverRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeGenreRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRestrictedModeRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
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

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverTvScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeDiscoverRepository = FakeDiscoverRepository()
    private val fakeGenreRepository = FakeGenreRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun screenModel(
        discoverFilterRepository: FakeDiscoverFilterRepository = FakeDiscoverFilterRepository(),
        restrictedModeRepository: FakeRestrictedModeRepository = FakeRestrictedModeRepository(),
    ) = DiscoverTvScreenModel(fakeDiscoverRepository, fakeGenreRepository, discoverFilterRepository, restrictedModeRepository)

    @Test
    fun testStartsAlreadyLoadedUsingPersistedFilters() =
        runTest(testDispatcher) {
            val persisted = DiscoverFilters(genreIds = setOf(10759), year = 2021)
            fakeDiscoverRepository.discoverTvShowsResult =
                Result.success(TvPageResult(page = 1, list = listOf(Tv(id = 1, title = "Show A")), totalResults = 1, totalPages = 1))

            val viewModel = screenModel(FakeDiscoverFilterRepository(tvFilters = persisted))

            assertEquals(persisted, viewModel.filters)
            assertEquals(listOf(Tv(id = 1, title = "Show A")), viewModel.tvList)
            assertEquals(listOf(Triple(1, persisted, false)), fakeDiscoverRepository.discoverTvShowsCalls)
        }

    @Test
    fun testApplyFiltersPersistsResetsToPageOneAndReloads() =
        runTest(testDispatcher) {
            fakeDiscoverRepository.discoverTvShowsResult =
                Result.success(TvPageResult(page = 1, list = listOf(Tv(id = 1, title = "Old")), totalResults = 1, totalPages = 2))
            val filterRepository = FakeDiscoverFilterRepository()
            val viewModel = screenModel(filterRepository)

            val newFilters = DiscoverFilters(genreIds = setOf(16), year = 2015)
            fakeDiscoverRepository.discoverTvShowsResult =
                Result.success(TvPageResult(page = 1, list = listOf(Tv(id = 2, title = "New")), totalResults = 1, totalPages = 0))
            viewModel.applyFilters(newFilters)

            assertEquals(newFilters, viewModel.filters)
            assertEquals(listOf(newFilters), filterRepository.setTvFiltersCalls)
            assertEquals(listOf(Tv(id = 2, title = "New")), viewModel.tvList)
            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
        }

    @Test
    fun testNetworkErrorSetsNetworkErrorState() =
        runTest(testDispatcher) {
            fakeDiscoverRepository.discoverTvShowsResult = Result.failure(IOException("boom"))

            val viewModel = screenModel()

            assertEquals(ListState.NETWORK_ERROR, viewModel.listState)
            assertTrue(viewModel.tvList.isEmpty())
        }
}
