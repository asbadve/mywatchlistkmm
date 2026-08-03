package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.TrailerSource
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
class TrendingTrailersFeatureFlagTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeTrendingRepository = FakeTrendingRepository()
    private val fakeMovieRepository = FakeMovieRepository()
    private val fakeTvRepository = FakeTvRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTrendingRepository.getTrendingResult =
            Result.success(MoviePageResult(page = 1, list = listOf(Movie(id = 1, title = "Trend")), totalResults = 1, totalPages = 1))
        // A trailer is available if anything asks - the point of the assertions below is that nothing does.
        fakeMovieRepository.getMoviesResult =
            Result.success(MoviePageResult(page = 1, list = listOf(Movie(id = 9, title = "Movie")), totalResults = 1, totalPages = 1))
        fakeMovieRepository.getMovieVideosResults[9L] =
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

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTrailersDisabled_skipsTrailerFetchAtInit() =
        runTest(testDispatcher) {
            val viewModel =
                TrendingScreenTabViewModel(
                    fakeTrendingRepository,
                    fakeMovieRepository,
                    fakeTvRepository,
                    trailersEnabled = false,
                )

            // Trending media still loads; the trailer rail's fan-out (source list + per-title videos) does not.
            assertEquals(3, fakeTrendingRepository.getTrendingCalls.size)
            assertTrue(fakeMovieRepository.getMoviesCalls.isEmpty(), "No trailer source list should be fetched when trailers are off")
            assertTrue(fakeMovieRepository.getMovieVideosCalls.isEmpty(), "No per-title video calls should fire when trailers are off")
            assertTrue(viewModel.trailerList.value.isEmpty())
        }

    @Test
    fun testTrailersEnabled_fetchesTrailersAtInit() =
        runTest(testDispatcher) {
            val viewModel =
                TrendingScreenTabViewModel(
                    fakeTrendingRepository,
                    fakeMovieRepository,
                    fakeTvRepository,
                    trailersEnabled = true,
                )

            assertEquals(TrailerSource.IN_THEATERS, viewModel.selectedTrailerSource.value)
            assertEquals(listOf(1 to "now_playing"), fakeMovieRepository.getMoviesCalls)
            assertEquals(listOf("Movie"), viewModel.trailerList.value.map { it.mediaTitle })
        }
}
