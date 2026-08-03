package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
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
class TvShowScreenTabsUiTest {
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
    // (TvListScreenModel.kt) stays true forever and, with only one item in a small grid,
    // CommonTvShowListScreenContent's near-end-of-list pagination trigger keeps re-firing
    // loadTvShows() in an unbounded loop.
    private fun fakeRepoWithShow(title: String) =
        FakeTvRepository().apply {
            getTvShowsResult =
                Result.success(
                    TvPageResult(
                        page = 1,
                        list = listOf(Tv(id = 1, title = title, originalTitle = title)),
                        totalResults = 1,
                        totalPages = 0,
                    ),
                )
        }

    @Test
    fun testOnlySelectedSubTabLoads_andRevisitingDoesNotRefetch() =
        runComposeUiTest {
            val airingTodayRepo = fakeRepoWithShow("Airing Today Show")
            val onTheAirRepo = fakeRepoWithShow("On The Air Show")
            val popularRepo = fakeRepoWithShow("Popular Show")
            val topRatedRepo = fakeRepoWithShow("Top Rated Show")

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
                    TvShowScreenTabs(
                        onTvShowSelected = {},
                        airingTodayRepository = airingTodayRepo,
                        onTheAirRepository = onTheAirRepo,
                        popularRepository = popularRepo,
                        topRatedRepository = topRatedRepo,
                    )
                }
            }

            // Only the initially-selected "Airing Today" sub-tab should have fetched.
            onAllNodesWithText("Airing Today Show")[0].assertExists()
            assertEquals(1, airingTodayRepo.getTvShowsCalls.size)
            assertEquals(0, onTheAirRepo.getTvShowsCalls.size)
            assertEquals(0, popularRepo.getTvShowsCalls.size)
            assertEquals(0, topRatedRepo.getTvShowsCalls.size)

            // Selecting "On The Air" for the first time loads it, and only it.
            onNodeWithText("On The Air").performClick()
            onAllNodesWithText("On The Air Show")[0].assertExists()
            assertEquals(1, onTheAirRepo.getTvShowsCalls.size)
            assertEquals(0, popularRepo.getTvShowsCalls.size)
            assertEquals(0, topRatedRepo.getTvShowsCalls.size)

            // Revisiting "Airing Today" does not trigger another fetch.
            onNodeWithText("Airing Today").performClick()
            onAllNodesWithText("Airing Today Show")[0].assertExists()
            assertEquals(1, airingTodayRepo.getTvShowsCalls.size)
        }
}
