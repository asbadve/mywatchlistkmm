package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.TrailerSource
import com.ajinkyabadve.kmmmywatchlist.features.trending.repository.FakeTrailerCacheRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class TrendingScreenContentTrailersUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // A ViewModel with a trailer already loaded, so the only thing under test is whether the
    // showTrailers flag decides to render the rail.
    private fun viewModelWithTrailer(videosGate: CompletableDeferred<Unit>? = null): TrendingScreenTabViewModel {
        val movieRepository =
            FakeMovieRepository().apply {
                videosGate?.let { getMovieVideosGates[MOVIE_ID] = it }
                getMoviesResult =
                    Result.success(
                        MoviePageResult(
                            page = 1,
                            list = listOf(Movie(id = MOVIE_ID.toInt(), title = MOVIE_TITLE)),
                            totalResults = 1,
                            totalPages = 1,
                        ),
                    )
                getMovieVideosResults[MOVIE_ID] =
                    Result.success(
                        VideoResponse(
                            results =
                                listOf(
                                    VideoResult(
                                        id = "v",
                                        key = "k",
                                        site = "YouTube",
                                        type = "Trailer",
                                        official = true,
                                        publishedAt = "2026-01-01T00:00:00.000Z",
                                    ),
                                ),
                        ),
                    )
            }
        return TrendingScreenTabViewModel(
            FakeTrendingRepository(),
            movieRepository,
            FakeTvRepository(),
            trailersEnabled = true,
            trailerCacheRepository = FakeTrailerCacheRepository(),
        )
    }

    @Test
    fun testTrailersHidden_whenShowTrailersFalse() =
        runComposeUiTest {
            setContent {
                TrendingScreenContent(
                    screenLoadingState = false,
                    sections = emptyList(),
                    onChipSelected = { _, _ -> },
                    onMovieSelected = {},
                    trailersViewModel = viewModelWithTrailer(),
                    showTrailers = false,
                )
            }

            onNodeWithText("Latest Trailers").assertDoesNotExist()
        }

    @Test
    fun testTrailersShown_whenShowTrailersTrue() =
        runComposeUiTest {
            setContent {
                TrendingScreenContent(
                    screenLoadingState = false,
                    sections = emptyList(),
                    onChipSelected = { _, _ -> },
                    onMovieSelected = {},
                    trailersViewModel = viewModelWithTrailer(),
                    showTrailers = true,
                )
            }

            onNodeWithText("Latest Trailers").assertIsDisplayed()
        }

    @Test
    fun testWhileLoading_theRailShowsPlaceholderCardsThenTheTrailer() =
        runComposeUiTest {
            val gate = CompletableDeferred<Unit>()
            val viewModel = viewModelWithTrailer(videosGate = gate)
            setContent {
                TrendingScreenContent(
                    screenLoadingState = false,
                    sections = emptyList(),
                    onChipSelected = { _, _ -> },
                    onMovieSelected = {},
                    trailersViewModel = viewModel,
                    showTrailers = true,
                )
            }

            onAllNodesWithTag(LatestTrailersConstant.PLACEHOLDER_TAG).assertCountEquals(LatestTrailersConstant.EMPTY_PLACEHOLDER_COUNT)
            gate.complete(Unit)

            waitUntil { onAllNodesWithText(MOVIE_TITLE).fetchSemanticsNodes().isNotEmpty() }
            onAllNodesWithTag(LatestTrailersConstant.PLACEHOLDER_TAG).assertCountEquals(0)
        }

    // Checks the newest card is on screen after a chip switch. It does NOT reproduce the rail drift
    // fixed in LatestTrailersSection (scroll anchored to an old item as cards are inserted in front):
    // that showed on an Android emulator but not in the desktop test harness, and was verified on
    // device (2026-10-04). Don't rely on this test alone when touching the rail's scroll handling.
    @Test
    fun testSwitchingChips_theNewRailStartsAtItsNewestCard() =
        runComposeUiTest {
            val olderGate = CompletableDeferred<Unit>()
            val newerGate = CompletableDeferred<Unit>()

            fun page(vararg movies: Movie) =
                Result.success(MoviePageResult(page = 1, list = movies.toList(), totalResults = movies.size, totalPages = 1))

            fun trailer(
                id: Long,
                published: String,
            ) = Result.success(
                VideoResponse(
                    results =
                        listOf(
                            VideoResult(
                                id = "v$id",
                                key = "k$id",
                                site = "YouTube",
                                type = "Trailer",
                                official = true,
                                publishedAt = published,
                            ),
                        ),
                ),
            )
            val movieRepository =
                FakeMovieRepository().apply {
                    getMoviesResultsByFetchType[MoviesConstant.NOW_PLAYING_API_PATH] =
                        page(Movie(id = THEATERS_ID.toInt(), title = THEATERS_TITLE))
                    getMoviesResultsByFetchType[MoviesConstant.UPCOMING_API_PATH] =
                        page(Movie(id = OLDER_ID.toInt(), title = OLDER_TITLE), Movie(id = NEWER_ID.toInt(), title = NEWER_TITLE))
                    getMovieVideosResults[THEATERS_ID] = trailer(THEATERS_ID, OLDER_PUBLISHED)
                    getMovieVideosResults[OLDER_ID] = trailer(OLDER_ID, OLDER_PUBLISHED)
                    getMovieVideosResults[NEWER_ID] = trailer(NEWER_ID, NEWER_PUBLISHED)
                    getMovieVideosGates[OLDER_ID] = olderGate
                    getMovieVideosGates[NEWER_ID] = newerGate
                }
            val viewModel =
                TrendingScreenTabViewModel(
                    FakeTrendingRepository(),
                    movieRepository,
                    FakeTvRepository(),
                    trailersEnabled = true,
                    trailerCacheRepository = FakeTrailerCacheRepository(),
                )
            setContent {
                // Phone-narrow, so one card fills the rail and a drifted rail really hides the newest card.
                Box(modifier = Modifier.width(PHONE_WIDTH)) {
                    TrendingScreenContent(
                        screenLoadingState = false,
                        sections = emptyList(),
                        onChipSelected = { _, _ -> },
                        onMovieSelected = {},
                        trailersViewModel = viewModel,
                        showTrailers = true,
                    )
                }
            }
            waitUntil { onAllNodesWithText(THEATERS_TITLE).fetchSemanticsNodes().isNotEmpty() }

            viewModel.onTrailerSourceSelected(TrailerSource.UPCOMING)
            olderGate.complete(Unit)
            waitUntil { onAllNodesWithText(OLDER_TITLE, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            // Newest-first: this card is inserted ahead of the one already shown.
            newerGate.complete(Unit)
            waitForIdle()

            onNodeWithText(NEWER_TITLE).assertIsDisplayed()
        }

    private companion object {
        const val MOVIE_ID = 9L
        const val MOVIE_TITLE = "Movie"
        const val OLDER_ID = 21L
        const val THEATERS_ID = 31L
        const val THEATERS_TITLE = "In Theaters Now"
        val PHONE_WIDTH = 320.dp
        const val NEWER_ID = 22L
        const val OLDER_TITLE = "Older Release"
        const val NEWER_TITLE = "Newer Release"
        const val OLDER_PUBLISHED = "2026-01-01T00:00:00.000Z"
        const val NEWER_PUBLISHED = "2026-02-01T00:00:00.000Z"
    }
}
