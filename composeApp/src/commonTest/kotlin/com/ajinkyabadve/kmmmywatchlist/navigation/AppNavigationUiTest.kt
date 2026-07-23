package com.ajinkyabadve.kmmmywatchlist.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.ajinkyabadve.kmmmywatchlist.MainAppScreen
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.MovieListScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.category.PersonListScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.FakeTrendingRepository
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.TrendingScreenTabViewModel
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category.TvListScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class AppNavigationUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAppLaunch_defaultsToTrendingAndNavigationFlow() =
        runComposeUiTest {
            val fakeTrendingRepository =
                FakeTrendingRepository().apply {
                    getTrendingResult =
                        Result.success(
                            MoviePageResult(
                                page = 1,
                                list =
                                    listOf(
                                        Movie(id = 1, title = "Trending Movie X", posterPath = "/pathX.jpg"),
                                    ),
                                totalResults = 1,
                                totalPages = 1,
                            ),
                        )
                }
            val trendingViewModel = TrendingScreenTabViewModel(fakeTrendingRepository)

            val fakeMovieRepository = FakeMovieRepository()
            val nowPlayingViewModel = MovieListScreenModel(MoviesConstant.NOW_PLAYING_API_PATH, fakeMovieRepository)
            val upcomingViewModel = MovieListScreenModel(MoviesConstant.UPCOMING_API_PATH, fakeMovieRepository)
            val popularViewModel = MovieListScreenModel(MoviesConstant.POPULAR_API_PATH, fakeMovieRepository)
            val topRatedViewModel = MovieListScreenModel(MoviesConstant.TOP_RATED_API_PATH, fakeMovieRepository)

            val fakeTvRepository =
                com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen
                    .FakeTvRepository()
            val airingTodayTvViewModel =
                TvListScreenModel(
                    com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowsConstant.AIRING_TODAY_API_PATH,
                    fakeTvRepository,
                )
            val onTheAirTvViewModel =
                TvListScreenModel(
                    com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowsConstant.ON_THE_AIR_API_PATH,
                    fakeTvRepository,
                )
            val popularTvViewModel =
                TvListScreenModel(
                    com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowsConstant.POPULAR_API_PATH,
                    fakeTvRepository,
                )
            val topRatedTvViewModel =
                TvListScreenModel(
                    com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowsConstant.TOP_RATED_API_PATH,
                    fakeTvRepository,
                )

            val fakePersonRepository =
                com.ajinkyabadve.kmmmywatchlist.features.person.screen
                    .FakePersonRepository()
            val personListViewModel = PersonListScreenModel(fakePersonRepository)

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
                    MainAppScreen(
                        windowSize = WindowSize.COMPACT,
                        nowPlayingViewModel = nowPlayingViewModel,
                        upcomingViewModel = upcomingViewModel,
                        popularViewModel = popularViewModel,
                        topRatedViewModel = topRatedViewModel,
                        trendingViewModel = trendingViewModel,
                        airingTodayTvViewModel = airingTodayTvViewModel,
                        onTheAirTvViewModel = onTheAirTvViewModel,
                        popularTvViewModel = popularTvViewModel,
                        topRatedTvViewModel = topRatedTvViewModel,
                        personListViewModel = personListViewModel,
                    )
                }
            }

            // 1. Verify Trending Screen displays correctly by default
            onNodeWithText("Trending Movies").assertExists()
            onNodeWithText("Trending Tv show").assertExists()
            onNodeWithText("Trending People").assertExists()
            onAllNodesWithText("Trending Movie X").assertCountEquals(3)

            // 2. Navigate to Movies Tab and check now playing
            onNodeWithContentDescription("Movies", useUnmergedTree = true).performClick()
            onAllNodesWithText("Now Playing Movie A")[0].assertExists()

            // Click Movie sub-tabs and verify content updates
            onNodeWithText("Upcoming").performClick()
            onAllNodesWithText("Upcoming Movie B")[0].assertExists()

            onNodeWithText("Popular").performClick()
            onAllNodesWithText("Popular Movie C")[0].assertExists()

            onNodeWithText("Top Rated").performClick()
            onAllNodesWithText("Top Rated Movie D")[0].assertExists()

            // Go back to Now Playing sub-tab
            onNodeWithText("Now Playing").performClick()
            onAllNodesWithText("Now Playing Movie A")[0].assertExists()

            // 3. Navigate to Tv Shows Tab
            onNodeWithContentDescription("Tv shows", useUnmergedTree = true).performClick()
            onAllNodesWithText("Tv Show A")[0].assertExists()

            // 4. Navigate to Person Tab
            onNodeWithContentDescription("Person", useUnmergedTree = true).performClick()
            onAllNodesWithText("Person A")[0].assertExists()

            // 5. Navigate to My Fav Tab
            onNodeWithContentDescription("My Fav", useUnmergedTree = true).performClick()
            onNodeWithText("Coming Soon").assertExists()

            // 6. Navigate back to Trending Tab to verify navigation state is intact
            onNodeWithContentDescription("Trending", useUnmergedTree = true).performClick()
            onAllNodesWithText("Trending Movie X").assertCountEquals(3)
        }
}
