package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionDetailScreenModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakeMovieRepository()

    private lateinit var notFoundException: HttpExceptions

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runTest {
            notFoundException = HttpExceptionsTestFactory.create(HttpStatusCode.NotFound)
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSuccessReturnsCollectionWithParts() = runTest(testDispatcher) {
        val collection = CollectionDetail(
            id = 263,
            name = "The Dark Knight Collection",
            parts = listOf(Movie(id = 155, title = "The Dark Knight")),
        )
        fakeRepository.getCollectionDetailsResult = Result.success(collection)

        val viewModel = CollectionDetailScreenModel(263, fakeRepository)

        val state = assertIs<CollectionDetailState.Success>(viewModel.uiState.value)
        assertEquals(collection, state.collection)
        assertEquals("The Dark Knight", state.collection.parts.single().title)
        assertEquals(listOf(263L), fakeRepository.getCollectionDetailsCalls)
    }

    @Test
    fun testHttpExceptionsSetsErrorWithResponseMessage() = runTest(testDispatcher) {
        fakeRepository.getCollectionDetailsResult = Result.failure(notFoundException)

        val viewModel = CollectionDetailScreenModel(263, fakeRepository)

        val state = assertIs<CollectionDetailState.Error>(viewModel.uiState.value)
        assertEquals(notFoundException.message, state.message)
    }

    @Test
    fun testIOExceptionSetsNetworkErrorMessage() = runTest(testDispatcher) {
        fakeRepository.getCollectionDetailsResult = Result.failure(IOException("Mock network failure"))

        val viewModel = CollectionDetailScreenModel(263, fakeRepository)

        val state = assertIs<CollectionDetailState.Error>(viewModel.uiState.value)
        assertEquals("Network Connection Error. Please check your internet connectivity.", state.message)
    }

    @Test
    fun testRetryAfterErrorSucceeds() = runTest(testDispatcher) {
        fakeRepository.getCollectionDetailsResult = Result.failure(IOException("Mock network failure"))
        val viewModel = CollectionDetailScreenModel(263, fakeRepository)
        assertIs<CollectionDetailState.Error>(viewModel.uiState.value)

        val collection = CollectionDetail(id = 263, name = "The Dark Knight Collection")
        fakeRepository.getCollectionDetailsResult = Result.success(collection)
        viewModel.loadCollectionDetails()

        val state = assertIs<CollectionDetailState.Success>(viewModel.uiState.value)
        assertEquals(collection, state.collection)
    }
}
