package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeDiscoverFilterRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeDiscoverRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeGenreRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRestrictedModeRepository
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
class DiscoverMovieScreenModelTest {
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
    ) = DiscoverMovieScreenModel(fakeDiscoverRepository, fakeGenreRepository, discoverFilterRepository, restrictedModeRepository)

    @Test
    fun testStartsAlreadyLoadedUsingPersistedFilters() =
        runTest(testDispatcher) {
            val persisted = DiscoverFilters(genreIds = setOf(28), year = 2020, sortBy = "vote_average.desc")
            fakeDiscoverRepository.discoverMoviesResult =
                Result.success(MoviePageResult(page = 1, list = listOf(Movie(id = 1, title = "Movie A")), totalResults = 1, totalPages = 1))

            val viewModel = screenModel(FakeDiscoverFilterRepository(movieFilters = persisted))

            assertEquals(persisted, viewModel.filters)
            assertEquals(listOf(Movie(id = 1, title = "Movie A")), viewModel.movieList)
            assertEquals(listOf(Triple(1, persisted, false)), fakeDiscoverRepository.discoverMoviesCalls)
        }

    @Test
    fun testApplyFiltersPersistsResetsToPageOneAndReloads() =
        runTest(testDispatcher) {
            fakeDiscoverRepository.discoverMoviesResult =
                Result.success(MoviePageResult(page = 1, list = listOf(Movie(id = 1, title = "Old")), totalResults = 1, totalPages = 2))
            val filterRepository = FakeDiscoverFilterRepository()
            val viewModel = screenModel(filterRepository)

            val newFilters = DiscoverFilters(genreIds = setOf(35), year = 2018)
            fakeDiscoverRepository.discoverMoviesResult =
                Result.success(MoviePageResult(page = 1, list = listOf(Movie(id = 2, title = "New")), totalResults = 1, totalPages = 0))
            viewModel.applyFilters(newFilters)

            assertEquals(newFilters, viewModel.filters)
            assertEquals(listOf(newFilters), filterRepository.setMovieFiltersCalls)
            assertEquals(listOf(Movie(id = 2, title = "New")), viewModel.movieList)
            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
        }

    @Test
    fun testRestrictedModeDisabledThreadsIncludeAdultTrue() =
        runTest(testDispatcher) {
            fakeDiscoverRepository.discoverMoviesResult =
                Result.success(MoviePageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 1))

            screenModel(restrictedModeRepository = FakeRestrictedModeRepository(restrictedModeEnabled = false))

            assertEquals(true, fakeDiscoverRepository.discoverMoviesCalls.first().third)
        }

    @Test
    fun testNetworkErrorSetsNetworkErrorState() =
        runTest(testDispatcher) {
            fakeDiscoverRepository.discoverMoviesResult = Result.failure(IOException("boom"))

            val viewModel = screenModel()

            assertEquals(ListState.NETWORK_ERROR, viewModel.listState)
            assertTrue(viewModel.movieList.isEmpty())
        }
}
