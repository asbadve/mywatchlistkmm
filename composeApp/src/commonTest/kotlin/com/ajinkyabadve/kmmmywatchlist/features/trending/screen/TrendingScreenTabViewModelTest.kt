package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.trending.repository.TrendingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
class TrendingScreenTabViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeTrendingRepository()

    private val testMovieResult = MoviePageResult(
        page = 1,
        list = listOf(Movie(id = 1, title = "Mock Movie")),
        totalResults = 1,
        totalPages = 1,
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Reset fake repository state
        fakeRepository.getTrendingCalls.clear()
        fakeRepository.getTrendingResult = Result.success(testMovieResult)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitializationLoadsAllMediaTypes() = runTest(testDispatcher) {
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
    fun testOnChipSelectedLoadsMovieWithNewTimeWindow() = runTest(testDispatcher) {
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
    fun testOnChipSelectedLoadsTvWithNewTimeWindow() = runTest(testDispatcher) {
        val viewModel = TrendingScreenTabViewModel(fakeRepository)
        
        // Clear initialization calls
        fakeRepository.getTrendingCalls.clear()
        
        viewModel.onChipSelected(1, MEDIA_TYPE_TV)

        assertEquals(1, viewModel.selectedTvChipIndex.value)
        
        assertEquals(1, fakeRepository.getTrendingCalls.size)
        assertEquals("week" to MEDIA_TYPE_TV, fakeRepository.getTrendingCalls[0])
    }

    @Test
    fun testOnChipSelectedLoadsPeopleWithNewTimeWindow() = runTest(testDispatcher) {
        val viewModel = TrendingScreenTabViewModel(fakeRepository)
        
        // Clear initialization calls
        fakeRepository.getTrendingCalls.clear()
        
        viewModel.onChipSelected(1, MEDIA_TYPE_PEOPLE)

        assertEquals(1, viewModel.selectedPeopleChipIndex.value)
        
        assertEquals(1, fakeRepository.getTrendingCalls.size)
        assertEquals("week" to MEDIA_TYPE_PEOPLE, fakeRepository.getTrendingCalls[0])
    }

    @Test
    fun testExceptionHandlingDoesNotCrash() = runTest(testDispatcher) {
        fakeRepository.getTrendingResult = Result.failure(io.ktor.utils.io.errors.IOException("Mock network failure"))

        val viewModel = TrendingScreenTabViewModel(fakeRepository)
        
        // Eager dispatcher runs setup, catches exception, and executes finally block
        assertFalse(viewModel.isScreenLoading.value)
    }

    @Test
    fun testExceptionHandlingSetsCorrectErrorMessage() = runTest(testDispatcher) {
        fakeRepository.getTrendingResult = Result.failure(io.ktor.utils.io.errors.IOException("Mock network failure"))

        val viewModel = TrendingScreenTabViewModel(fakeRepository)
        
        assertEquals("Network error. Please check your connection.", viewModel.movieTrendError.value)
        assertEquals("Network error. Please check your connection.", viewModel.tvTrendError.value)
        assertEquals("Network error. Please check your connection.", viewModel.peopleTrendError.value)
    }

    private companion object {
        const val MEDIA_TYPE_MOVIE = "movie"
        const val MEDIA_TYPE_TV = "tv"
        const val MEDIA_TYPE_PEOPLE = "person"
    }
}
