package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeDiscoverFilterRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeDiscoverRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeGenreRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRestrictedModeRepository
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

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DiscoverTvTabUiTest {
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
    fun testResultsAlreadyLoadedOnFirstOpen() =
        runComposeUiTest {
            val discoverRepository = FakeDiscoverRepository()
            discoverRepository.discoverTvShowsResult =
                Result.success(
                    TvPageResult(page = 1, list = listOf(Tv(id = 1, title = "Fake Show")), totalResults = 1, totalPages = 0),
                )
            val tvModel =
                DiscoverTvScreenModel(
                    discoverRepository = discoverRepository,
                    genreRepository = FakeGenreRepository(tvGenres = listOf(Genre(id = 10759, name = "Action & Adventure"))),
                    discoverFilterRepository = FakeDiscoverFilterRepository(),
                    restrictedModeRepository = FakeRestrictedModeRepository(),
                )

            setContent { DiscoverTvTab(screenModel = tvModel) }

            onNodeWithText("Fake Show").assertExists()
        }

    @Test
    fun testUpcomingTvShowShowsUpcomingBadge() =
        runComposeUiTest {
            val discoverRepository = FakeDiscoverRepository()
            discoverRepository.discoverTvShowsResult =
                Result.success(
                    TvPageResult(
                        page = 1,
                        list =
                            listOf(
                                Tv(id = 1, title = "Released Show", firstAirDate = "2000-01-01"),
                                Tv(id = 2, title = "Upcoming Show", firstAirDate = "2099-01-01"),
                            ),
                        totalResults = 2,
                        totalPages = 0,
                    ),
                )
            val tvModel =
                DiscoverTvScreenModel(
                    discoverRepository = discoverRepository,
                    genreRepository = FakeGenreRepository(tvGenres = listOf(Genre(id = 10759, name = "Action & Adventure"))),
                    discoverFilterRepository = FakeDiscoverFilterRepository(),
                    restrictedModeRepository = FakeRestrictedModeRepository(),
                )

            setContent { DiscoverTvTab(screenModel = tvModel) }

            onNodeWithText("Upcoming").assertExists()
        }
}
