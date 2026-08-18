package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeDiscoverFilterRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeDiscoverRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeGenreRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.DiscoverMovieScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRestrictedModeRepository
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
class MovieScreenTabsUiTest {
    private companion object {
        const val MANY_MOVIES_COUNT = 30
    }

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // totalPages = 0 marks pagination as exhausted after the first load - otherwise the fake's
    // page always reports back as 1, so `canPaginate = response.page <= response.totalPages`
    // (MovieListScreenModel.kt) stays true forever and, with only one item in a small grid,
    // CommonMovieListScreenContent's near-end-of-list pagination trigger keeps re-firing
    // loadMovies() in an unbounded loop.
    private fun fakeRepoWithMovie(title: String) =
        FakeMovieRepository().apply {
            getMoviesResult =
                Result.success(
                    MoviePageResult(page = 1, list = listOf(Movie(id = 1, title = title)), totalResults = 1, totalPages = 0),
                )
        }

    @Test
    fun testOnlySelectedSubTabLoads_andRevisitingDoesNotRefetch() =
        runComposeUiTest {
            val nowPlayingRepo = fakeRepoWithMovie("Now Playing Movie A")
            val upcomingRepo = fakeRepoWithMovie("Upcoming Movie B")
            val popularRepo = fakeRepoWithMovie("Popular Movie C")
            val topRatedRepo = fakeRepoWithMovie("Top Rated Movie D")

            setContent {
                val lifecycleOwner =
                    remember {
                        object : LifecycleOwner {
                            override val lifecycle: Lifecycle =
                                LifecycleRegistry(this).apply {
                                    currentState = Lifecycle.State.RESUMED
                                }
                        }
                    }
                val viewModelStoreOwner =
                    remember {
                        object : ViewModelStoreOwner {
                            override val viewModelStore: ViewModelStore = ViewModelStore()
                        }
                    }

                CompositionLocalProvider(
                    LocalLifecycleOwner provides lifecycleOwner,
                    LocalViewModelStoreOwner provides viewModelStoreOwner,
                ) {
                    MovieScreenTabs(
                        onMovieSelected = {},
                        nowPlayingRepository = nowPlayingRepo,
                        upcomingRepository = upcomingRepo,
                        popularRepository = popularRepo,
                        topRatedRepository = topRatedRepo,
                    )
                }
            }

            // Only the initially-selected "Now Playing" sub-tab should have fetched.
            onAllNodesWithText("Now Playing Movie A")[0].assertExists()
            assertEquals(1, nowPlayingRepo.getMoviesCalls.size)
            assertEquals(0, upcomingRepo.getMoviesCalls.size)
            assertEquals(0, popularRepo.getMoviesCalls.size)
            assertEquals(0, topRatedRepo.getMoviesCalls.size)

            // Selecting "Upcoming" for the first time loads it, and only it.
            onNodeWithText("Upcoming").performClick()
            onAllNodesWithText("Upcoming Movie B")[0].assertExists()
            assertEquals(1, upcomingRepo.getMoviesCalls.size)
            assertEquals(0, popularRepo.getMoviesCalls.size)
            assertEquals(0, topRatedRepo.getMoviesCalls.size)

            // Revisiting "Now Playing" does not trigger another fetch.
            onNodeWithText("Now Playing").performClick()
            onAllNodesWithText("Now Playing Movie A")[0].assertExists()
            assertEquals(1, nowPlayingRepo.getMoviesCalls.size)
        }

    @Test
    fun testRetappingAlreadySelectedTabScrollsGridBackToTop() =
        runComposeUiTest {
            val titles = (0 until MANY_MOVIES_COUNT).map { "Now Playing Movie $it" }
            val nowPlayingRepo =
                FakeMovieRepository().apply {
                    getMoviesResult =
                        Result.success(
                            MoviePageResult(
                                page = 1,
                                list = titles.mapIndexed { index, title -> Movie(id = index, title = title) },
                                totalResults = titles.size,
                                totalPages = 0,
                            ),
                        )
                }

            setContent {
                MovieScreenTabs(onMovieSelected = {}, nowPlayingRepository = nowPlayingRepo)
            }

            onNodeWithText(titles.first()).assertExists()

            // Scroll the grid down - the first item is recycled out of the semantics tree once it's
            // off-screen, which is what lets the assertion below tell scrolled-down from scrolled-up.
            onNode(hasScrollToIndexAction()).performScrollToIndex(titles.size - 1)
            onNodeWithText(titles.first()).assertDoesNotExist()

            // Re-tapping "Now Playing" while it's already selected should scroll back to the top
            // instead of doing nothing.
            onNodeWithText("Now Playing").performClick()
            onNodeWithText(titles.first()).assertExists()
        }

    @Test
    fun testDiscoverFabOnlyVisibleOnDiscoverTabAndShowsActiveFilterBadge() =
        runComposeUiTest {
            val discoverRepository =
                FakeDiscoverRepository().apply {
                    discoverMoviesResult = Result.success(MoviePageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0))
                }
            val discoverScreenModel =
                DiscoverMovieScreenModel(
                    discoverRepository = discoverRepository,
                    genreRepository = FakeGenreRepository(movieGenres = listOf(Genre(id = 28, name = "Action"))),
                    discoverFilterRepository =
                        FakeDiscoverFilterRepository(movieFilters = DiscoverFilters(genreIds = setOf(28))),
                    restrictedModeRepository = FakeRestrictedModeRepository(),
                )

            setContent {
                MovieScreenTabs(
                    onMovieSelected = {},
                    nowPlayingRepository = fakeRepoWithMovie("Now Playing Movie A"),
                    discoverScreenModel = discoverScreenModel,
                )
            }

            // Not shown while looking at "Now Playing" (the default first tab).
            onNodeWithText("Filters").assertDoesNotExist()

            onNodeWithText("Discover").performClick()

            // One genre pre-applied via the fake filter repository - badge shows "1". The FAB
            // merges its icon/text/badge into one clickable semantics node, so the individual
            // Text children are only visible via the unmerged tree.
            onNodeWithText("Filters", useUnmergedTree = true).assertExists()
            onNodeWithText("1", useUnmergedTree = true).assertExists()
        }

    @Test
    fun testApplyingADiscoverFilterPersistsAndClosesDialog() =
        runComposeUiTest {
            val discoverRepository =
                FakeDiscoverRepository().apply {
                    discoverMoviesResult = Result.success(MoviePageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0))
                }
            val filterRepository = FakeDiscoverFilterRepository()
            val discoverScreenModel =
                DiscoverMovieScreenModel(
                    discoverRepository = discoverRepository,
                    genreRepository = FakeGenreRepository(movieGenres = listOf(Genre(id = 28, name = "Action"))),
                    discoverFilterRepository = filterRepository,
                    restrictedModeRepository = FakeRestrictedModeRepository(),
                )

            setContent {
                MovieScreenTabs(
                    onMovieSelected = {},
                    nowPlayingRepository = fakeRepoWithMovie("Now Playing Movie A"),
                    discoverScreenModel = discoverScreenModel,
                )
            }

            onNodeWithText("Discover").performClick()
            onNodeWithText("Filters", useUnmergedTree = true).performClick()
            onNodeWithText("Action").performClick()
            onNodeWithText("Apply").performClick()

            assertEquals(setOf(28), filterRepository.setMovieFiltersCalls.last().genreIds)
            onNodeWithText("Apply").assertDoesNotExist()
        }
}
