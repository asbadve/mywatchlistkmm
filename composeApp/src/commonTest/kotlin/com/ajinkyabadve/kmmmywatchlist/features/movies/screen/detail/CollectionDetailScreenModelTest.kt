package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Credits
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.FakeMovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.CrewMember
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
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
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
    fun testSuccessReturnsCollectionWithParts() =
        runTest(testDispatcher) {
            val collection =
                CollectionDetail(
                    id = 263,
                    name = "The Dark Knight Collection",
                    parts = listOf(Movie(id = 155, title = "The Dark Knight")),
                )
            fakeRepository.getCollectionDetailsResult = Result.success(collection)

            val viewModel = CollectionDetailScreenModel(263, fakeRepository)

            val state = assertIs<CollectionDetailState.Success>(viewModel.uiState.value)
            assertEquals(collection, state.collection)
            assertEquals(
                "The Dark Knight",
                state.collection.parts
                    .single()
                    .title,
            )
            assertEquals(listOf(263L), fakeRepository.getCollectionDetailsCalls)
        }

    @Test
    fun testFeaturedCreditsAggregatedFromEveryPart() =
        runTest(testDispatcher) {
            val collection =
                CollectionDetail(
                    id = 263,
                    name = "The Dark Knight Collection",
                    parts = listOf(Movie(id = 272, title = "Batman Begins"), Movie(id = 155, title = "The Dark Knight")),
                )
            fakeRepository.getCollectionDetailsResult = Result.success(collection)
            fakeRepository.getMovieCreditsResults[272L] =
                Result.success(
                    Credits(
                        cast = listOf(CastMember(id = 1, name = "Christian Bale", character = "Bruce Wayne", order = 0)),
                        crew = listOf(CrewMember(id = 525, name = "Christopher Nolan", job = "Director", department = "Directing")),
                    ),
                )
            fakeRepository.getMovieCreditsResults[155L] =
                Result.success(
                    Credits(
                        cast = listOf(CastMember(id = 1, name = "Christian Bale", character = "Batman", order = 0)),
                        crew = listOf(CrewMember(id = 525, name = "Christopher Nolan", job = "Director", department = "Directing")),
                    ),
                )

            val viewModel = CollectionDetailScreenModel(263, fakeRepository)

            val state = assertIs<CollectionDetailState.Success>(viewModel.uiState.value)
            assertEquals(listOf(272L, 155L), fakeRepository.getMovieCreditsCalls)
            assertEquals("Christian Bale", state.featuredCast.single().name)
            assertEquals("Bruce Wayne / Batman", state.featuredCast.single().character)
            assertEquals("Christopher Nolan", state.featuredCrew.single().name)
            assertEquals("Director", state.featuredCrew.single().character)
        }

    @Test
    fun testFailedCreditsCallsDoNotBreakTheScreen() =
        runTest(testDispatcher) {
            val collection =
                CollectionDetail(
                    id = 263,
                    name = "The Dark Knight Collection",
                    parts = listOf(Movie(id = 272, title = "Batman Begins"), Movie(id = 155, title = "The Dark Knight")),
                )
            fakeRepository.getCollectionDetailsResult = Result.success(collection)
            fakeRepository.getMovieCreditsResults[272L] = Result.failure(IOException("boom"))
            fakeRepository.getMovieCreditsResults[155L] =
                Result.success(
                    Credits(cast = listOf(CastMember(id = 5, name = "Heath Ledger", character = "Joker", order = 1))),
                )

            val viewModel = CollectionDetailScreenModel(263, fakeRepository)

            val state = assertIs<CollectionDetailState.Success>(viewModel.uiState.value)
            assertEquals("Heath Ledger", state.featuredCast.single().name)
        }

    @Test
    fun testHttpExceptionsSetsErrorWithResponseMessage() =
        runTest(testDispatcher) {
            fakeRepository.getCollectionDetailsResult = Result.failure(notFoundException)

            val viewModel = CollectionDetailScreenModel(263, fakeRepository)

            val state = assertIs<CollectionDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Plain(notFoundException.message), state.message)
        }

    @Test
    fun testIOExceptionSetsNetworkErrorMessage() =
        runTest(testDispatcher) {
            fakeRepository.getCollectionDetailsResult = Result.failure(IOException("Mock network failure"))

            val viewModel = CollectionDetailScreenModel(263, fakeRepository)

            val state = assertIs<CollectionDetailState.Error>(viewModel.uiState.value)
            assertEquals(UiText.Resource(Res.string.error_network), state.message)
        }

    @Test
    fun testRetryAfterErrorSucceeds() =
        runTest(testDispatcher) {
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
