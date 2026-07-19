package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCombinedCredits
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCredit
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.FakePersonRepository
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
class PersonDetailScreenModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakePersonRepository()

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
    fun testSuccessReturnsPersonWithCredits() = runTest(testDispatcher) {
        val person = PersonDetail(
            id = 500,
            name = "Tom Cruise",
            knownForDepartment = "Acting",
            birthday = "1962-07-03",
            combinedCredits = PersonCombinedCredits(
                cast = listOf(PersonCredit(id = 180, title = "Minority Report", mediaType = "movie")),
            ),
        )
        fakeRepository.getPersonDetailsResult = Result.success(person)

        val viewModel = PersonDetailScreenModel(500, fakeRepository)

        val state = assertIs<PersonDetailState.Success>(viewModel.uiState.value)
        assertEquals(person, state.person)
        assertEquals("Minority Report", state.person.combinedCredits?.cast?.single()?.displayTitle)
        assertEquals(listOf(500L), fakeRepository.getPersonDetailsCalls)
    }

    @Test
    fun testHttpExceptionsSetsErrorWithResponseMessage() = runTest(testDispatcher) {
        fakeRepository.getPersonDetailsResult = Result.failure(notFoundException)

        val viewModel = PersonDetailScreenModel(500, fakeRepository)

        val state = assertIs<PersonDetailState.Error>(viewModel.uiState.value)
        assertEquals(notFoundException.message, state.message)
    }

    @Test
    fun testIOExceptionSetsNetworkErrorMessage() = runTest(testDispatcher) {
        fakeRepository.getPersonDetailsResult = Result.failure(IOException("Mock network failure"))

        val viewModel = PersonDetailScreenModel(500, fakeRepository)

        val state = assertIs<PersonDetailState.Error>(viewModel.uiState.value)
        assertEquals("Network Connection Error. Please check your internet connectivity.", state.message)
    }

    @Test
    fun testUnexpectedExceptionSetsGenericErrorMessage() = runTest(testDispatcher) {
        fakeRepository.getPersonDetailsResult = Result.failure(RuntimeException("Boom"))

        val viewModel = PersonDetailScreenModel(500, fakeRepository)

        val state = assertIs<PersonDetailState.Error>(viewModel.uiState.value)
        assertEquals("An unexpected error occurred while loading the person. Please try again.", state.message)
    }

    @Test
    fun testRetryAfterErrorSucceeds() = runTest(testDispatcher) {
        fakeRepository.getPersonDetailsResult = Result.failure(IOException("Mock network failure"))
        val viewModel = PersonDetailScreenModel(500, fakeRepository)
        assertIs<PersonDetailState.Error>(viewModel.uiState.value)

        val person = PersonDetail(id = 500, name = "Tom Cruise")
        fakeRepository.getPersonDetailsResult = Result.success(person)
        viewModel.loadPersonDetails()

        val state = assertIs<PersonDetailState.Success>(viewModel.uiState.value)
        assertEquals(person, state.person)
    }
}
