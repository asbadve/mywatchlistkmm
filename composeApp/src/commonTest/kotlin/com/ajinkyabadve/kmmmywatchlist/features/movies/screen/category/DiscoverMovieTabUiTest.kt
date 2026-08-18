package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeDiscoverFilterRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeDiscoverRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeGenreRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRestrictedModeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DiscoverMovieTabUiTest {
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
            discoverRepository.discoverMoviesResult =
                Result.success(
                    MoviePageResult(page = 1, list = listOf(Movie(id = 1, title = "Fake Movie")), totalResults = 1, totalPages = 0),
                )
            val movieModel =
                DiscoverMovieScreenModel(
                    discoverRepository = discoverRepository,
                    genreRepository = FakeGenreRepository(movieGenres = listOf(Genre(id = 28, name = "Action"))),
                    discoverFilterRepository = FakeDiscoverFilterRepository(),
                    restrictedModeRepository = FakeRestrictedModeRepository(),
                )

            setContent { DiscoverMovieTab(screenModel = movieModel) }

            onNodeWithText("Fake Movie").assertExists()
        }

    @Test
    fun testUpcomingMovieShowsUpcomingBadge() =
        runComposeUiTest {
            val discoverRepository = FakeDiscoverRepository()
            discoverRepository.discoverMoviesResult =
                Result.success(
                    MoviePageResult(
                        page = 1,
                        list =
                            listOf(
                                Movie(id = 1, title = "Released Movie", releaseDate = "2000-01-01"),
                                Movie(id = 2, title = "Upcoming Movie", releaseDate = "2099-01-01"),
                            ),
                        totalResults = 2,
                        totalPages = 0,
                    ),
                )
            val movieModel =
                DiscoverMovieScreenModel(
                    discoverRepository = discoverRepository,
                    genreRepository = FakeGenreRepository(movieGenres = listOf(Genre(id = 28, name = "Action"))),
                    discoverFilterRepository = FakeDiscoverFilterRepository(),
                    restrictedModeRepository = FakeRestrictedModeRepository(),
                )

            setContent { DiscoverMovieTab(screenModel = movieModel) }

            onNodeWithText("Upcoming").assertExists()
        }
}
