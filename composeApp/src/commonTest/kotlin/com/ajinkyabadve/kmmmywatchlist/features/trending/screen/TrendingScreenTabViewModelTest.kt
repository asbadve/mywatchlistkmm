package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.TrailerSource
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvPageResult
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.FakeTvRepository
import com.ajinkyabadve.kmmmywatchlist.network.HttpExceptionsTestFactory
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_trailers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
class TrendingScreenTabViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeTrendingRepository()
    private val fakeMovieRepository = FakeMovieRepository()
    private val fakeTvRepository = FakeTvRepository()

    private val testMovieResult =
        MoviePageResult(
            page = 1,
            list = listOf(Movie(id = 1, title = "Mock Movie")),
            totalResults = 1,
            totalPages = 1,
        )

    private lateinit var notFoundException: HttpExceptions

    private fun trailerVideo(
        id: String,
        publishedAt: String,
    ) = VideoResult(id = id, key = "key-$id", site = "YouTube", type = "Trailer", official = true, publishedAt = publishedAt)

    // Trailers are flag-parked (off by default); these tests exercise the still-present trailer
    // logic, so force it on. See TrendingTrailersFeatureFlagTest for the off-by-default behavior.
    private fun createViewModel() =
        TrendingScreenTabViewModel(fakeRepository, fakeMovieRepository, fakeTvRepository, trailersEnabled = true)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Reset fake repository state
        fakeRepository.getTrendingCalls.clear()
        fakeRepository.getTrendingResult = Result.success(testMovieResult)
        fakeMovieRepository.getMoviesResult =
            Result.success(MoviePageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 1))
        fakeTvRepository.getTvShowsResult =
            Result.success(TvPageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 1))
        runTest {
            notFoundException = HttpExceptionsTestFactory.create(HttpStatusCode.NotFound)
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitializationLoadsAllMediaTypes() =
        runTest(testDispatcher) {
            val viewModel = TrendingScreenTabViewModel(fakeRepository)

            // Eager dispatcher executes everything during initialization
            assertFalse(viewModel.isScreenLoading.value)
            assertEquals(testMovieResult.list, viewModel.trendMovieList.value)
            assertEquals(testMovieResult.list, viewModel.trendTvList.value)
            assertEquals(testMovieResult.list, viewModel.trendPeopleList.value)

            assertEquals(3, fakeRepository.getTrendingCalls.size)
            assertEquals("day" to MEDIA_TYPE_MOVIE, fakeRepository.getTrendingCalls[0])
            assertEquals("day" to MEDIA_TYPE_TV, fakeRepository.getTrendingCalls[1])
            assertEquals("day" to MEDIA_TYPE_PEOPLE, fakeRepository.getTrendingCalls[2])
        }

    @Test
    fun testOnChipSelectedLoadsMovieWithNewTimeWindow() =
        runTest(testDispatcher) {
            val viewModel = TrendingScreenTabViewModel(fakeRepository)

            // Clear initialization calls
            fakeRepository.getTrendingCalls.clear()

            viewModel.onChipSelected(1, MEDIA_TYPE_MOVIE)

            assertFalse(viewModel.isMovieTrendLoading.value)
            assertEquals(1, viewModel.selectedMovieChipIndex.value)

            assertEquals(1, fakeRepository.getTrendingCalls.size)
            assertEquals("week" to MEDIA_TYPE_MOVIE, fakeRepository.getTrendingCalls[0])
        }

    @Test
    fun testOnChipSelectedLoadsTvWithNewTimeWindow() =
        runTest(testDispatcher) {
            val viewModel = TrendingScreenTabViewModel(fakeRepository)

            // Clear initialization calls
            fakeRepository.getTrendingCalls.clear()

            viewModel.onChipSelected(1, MEDIA_TYPE_TV)

            assertEquals(1, viewModel.selectedTvChipIndex.value)

            assertEquals(1, fakeRepository.getTrendingCalls.size)
            assertEquals("week" to MEDIA_TYPE_TV, fakeRepository.getTrendingCalls[0])
        }

    @Test
    fun testOnChipSelectedLoadsPeopleWithNewTimeWindow() =
        runTest(testDispatcher) {
            val viewModel = TrendingScreenTabViewModel(fakeRepository)

            // Clear initialization calls
            fakeRepository.getTrendingCalls.clear()

            viewModel.onChipSelected(1, MEDIA_TYPE_PEOPLE)

            assertEquals(1, viewModel.selectedPeopleChipIndex.value)

            assertEquals(1, fakeRepository.getTrendingCalls.size)
            assertEquals("week" to MEDIA_TYPE_PEOPLE, fakeRepository.getTrendingCalls[0])
        }

    @Test
    fun testEmptyTrendingResultsResultInEmptyListsWithoutError() =
        runTest(testDispatcher) {
            fakeRepository.getTrendingResult =
                Result.success(
                    MoviePageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0),
                )

            val viewModel = TrendingScreenTabViewModel(fakeRepository)

            assertTrue(viewModel.trendMovieList.value.isEmpty())
            assertTrue(viewModel.trendTvList.value.isEmpty())
            assertTrue(viewModel.trendPeopleList.value.isEmpty())
            assertEquals(null, viewModel.movieTrendError.value)
            assertEquals(null, viewModel.tvTrendError.value)
            assertEquals(null, viewModel.peopleTrendError.value)
        }

    @Test
    fun testExceptionHandlingDoesNotCrash() =
        runTest(testDispatcher) {
            fakeRepository.getTrendingResult =
                Result.failure(
                    io.ktor.utils.io.errors
                        .IOException("Mock network failure"),
                )

            val viewModel = TrendingScreenTabViewModel(fakeRepository)

            // Eager dispatcher runs setup, catches exception, and executes finally block
            assertFalse(viewModel.isScreenLoading.value)
        }

    @Test
    fun testExceptionHandlingSetsCorrectErrorMessage() =
        runTest(testDispatcher) {
            fakeRepository.getTrendingResult =
                Result.failure(
                    io.ktor.utils.io.errors
                        .IOException("Mock network failure"),
                )

            val viewModel = TrendingScreenTabViewModel(fakeRepository)

            assertEquals("Network error. Please check your connection.", viewModel.movieTrendError.value)
            assertEquals("Network error. Please check your connection.", viewModel.tvTrendError.value)
            assertEquals("Network error. Please check your connection.", viewModel.peopleTrendError.value)
        }

    @Test
    fun testInitializationDefaultsToInTheatersAndPicksNewestTrailerPerMovie() =
        runTest(testDispatcher) {
            fakeMovieRepository.getMoviesResult =
                Result.success(
                    MoviePageResult(
                        page = 1,
                        list = listOf(Movie(id = 1, title = "Older Movie"), Movie(id = 2, title = "Newer Movie")),
                        totalResults = 2,
                        totalPages = 1,
                    ),
                )
            fakeMovieRepository.getMovieVideosResults[1L] =
                Result.success(VideoResponse(results = listOf(trailerVideo("older", "2026-01-01T00:00:00.000Z"))))
            fakeMovieRepository.getMovieVideosResults[2L] =
                Result.success(VideoResponse(results = listOf(trailerVideo("newer", "2026-02-01T00:00:00.000Z"))))

            val viewModel = createViewModel()

            assertEquals(TrailerSource.IN_THEATERS, viewModel.selectedTrailerSource.value)
            assertFalse(viewModel.isTrailerScreenLoading.value)
            assertEquals(listOf("Newer Movie", "Older Movie"), viewModel.trailerList.value.map { it.mediaTitle })
            assertEquals(listOf(1 to "now_playing"), fakeMovieRepository.getMoviesCalls)
        }

    @Test
    fun testOnTrailerSourceSelectedOnTvFetchesFromTvRepository() =
        runTest(testDispatcher) {
            fakeTvRepository.getTvShowsResult =
                Result.success(
                    TvPageResult(page = 1, list = listOf(Tv(id = 5, title = "A Show")), totalResults = 1, totalPages = 1),
                )
            fakeTvRepository.getTvVideosResults[5L] =
                Result.success(VideoResponse(results = listOf(trailerVideo("show-trailer", "2026-01-01T00:00:00.000Z"))))

            val viewModel = createViewModel()
            viewModel.onTrailerSourceSelected(TrailerSource.ON_TV)

            assertEquals(TrailerSource.ON_TV, viewModel.selectedTrailerSource.value)
            val trailer = viewModel.trailerList.value.single()
            assertEquals("A Show", trailer.mediaTitle)
            assertFalse(trailer.isMovie)
            assertEquals(listOf(5L), fakeTvRepository.getTvVideosCalls)
        }

    @Test
    fun testTitlesWithoutPlayableVideoAreDroppedNotCrashed() =
        runTest(testDispatcher) {
            fakeMovieRepository.getMoviesResult =
                Result.success(
                    MoviePageResult(
                        page = 1,
                        list = listOf(Movie(id = 1, title = "No Trailer"), Movie(id = 2, title = "Has Trailer")),
                        totalResults = 2,
                        totalPages = 1,
                    ),
                )
            fakeMovieRepository.getMovieVideosResults[1L] = Result.success(VideoResponse(results = emptyList()))
            fakeMovieRepository.getMovieVideosResults[2L] =
                Result.success(VideoResponse(results = listOf(trailerVideo("t", "2026-01-01T00:00:00.000Z"))))

            val viewModel = createViewModel()

            assertEquals(listOf("Has Trailer"), viewModel.trailerList.value.map { it.mediaTitle })
        }

    @Test
    fun testOneMovieFailingToFetchVideosDoesNotBreakTheRail() =
        runTest(testDispatcher) {
            fakeMovieRepository.getMoviesResult =
                Result.success(
                    MoviePageResult(
                        page = 1,
                        list = listOf(Movie(id = 1, title = "Fails"), Movie(id = 2, title = "Succeeds")),
                        totalResults = 2,
                        totalPages = 1,
                    ),
                )
            fakeMovieRepository.getMovieVideosResults[1L] = Result.failure(IOException("boom"))
            fakeMovieRepository.getMovieVideosResults[2L] =
                Result.success(VideoResponse(results = listOf(trailerVideo("t", "2026-01-01T00:00:00.000Z"))))

            val viewModel = createViewModel()

            assertEquals(listOf("Succeeds"), viewModel.trailerList.value.map { it.mediaTitle })
        }

    @Test
    fun testSwitchingTrailerSourceThenBackDoesNotRefetch() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onTrailerSourceSelected(TrailerSource.POPULAR)
            viewModel.onTrailerSourceSelected(TrailerSource.IN_THEATERS)

            assertEquals(listOf(1 to "now_playing", 1 to "popular"), fakeMovieRepository.getMoviesCalls)
        }

    @Test
    fun testTrailerHttpExceptionSetsErrorWithResponseMessage() =
        runTest(testDispatcher) {
            fakeMovieRepository.getMoviesResult = Result.failure(notFoundException)

            val viewModel = createViewModel()

            assertEquals(UiText.Plain(notFoundException.message), viewModel.trailerError.value)
        }

    @Test
    fun testTrailerIoExceptionSetsNetworkErrorMessage() =
        runTest(testDispatcher) {
            fakeMovieRepository.getMoviesResult = Result.failure(IOException("Mock network failure"))

            val viewModel = createViewModel()

            assertEquals(UiText.Resource(Res.string.error_network), viewModel.trailerError.value)
        }

    @Test
    fun testTrailerSerializationExceptionSetsGenericErrorMessage() =
        runTest(testDispatcher) {
            fakeMovieRepository.getMoviesResult = Result.failure(SerializationException("Boom"))

            val viewModel = createViewModel()

            assertEquals(UiText.Resource(Res.string.error_unexpected_trailers), viewModel.trailerError.value)
        }

    private companion object {
        const val MEDIA_TYPE_MOVIE = "movie"
        const val MEDIA_TYPE_TV = "tv"
        const val MEDIA_TYPE_PEOPLE = "person"
    }
}
