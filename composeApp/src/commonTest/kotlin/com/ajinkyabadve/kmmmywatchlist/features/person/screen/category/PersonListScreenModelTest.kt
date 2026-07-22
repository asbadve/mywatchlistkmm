package com.ajinkyabadve.kmmmywatchlist.features.person.screen.category

import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult
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
import kotlinx.serialization.SerializationException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PersonListScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepository = FakePersonRepository()

    private lateinit var badRequestException: HttpExceptions
    private lateinit var notFoundException: HttpExceptions

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runTest {
            badRequestException = HttpExceptionsTestFactory.create(HttpStatusCode.BadRequest)
            notFoundException = HttpExceptionsTestFactory.create(HttpStatusCode.NotFound)
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadPopulatesListAndAllowsPagination() =
        runTest(testDispatcher) {
            fakeRepository.getPopularPeopleResult =
                Result.success(
                    PersonPageResult(page = 1, list = listOf(Person(id = 1, name = "Person X")), totalResults = 1, totalPages = 2),
                )

            val viewModel = PersonListScreenModel(fakeRepository)

            assertEquals(ListState.IDLE, viewModel.listState)
            assertEquals(listOf(Person(id = 1, name = "Person X")), viewModel.personList)
            assertEquals(listOf(1), fakeRepository.getPopularPeopleCalls)
        }

    @Test
    fun testEmptyListResultSetsPaginationExhaust() =
        runTest(testDispatcher) {
            fakeRepository.getPopularPeopleResult =
                Result.success(
                    PersonPageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0),
                )

            val viewModel = PersonListScreenModel(fakeRepository)

            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)
            assertTrue(viewModel.personList.isEmpty())
        }

    @Test
    fun testLoadPopularPeopleAppendsNextPageWithoutClearingPreviousResults() =
        runTest(testDispatcher) {
            fakeRepository.getPopularPeopleResult =
                Result.success(
                    PersonPageResult(page = 1, list = listOf(Person(id = 1, name = "Person X")), totalResults = 2, totalPages = 2),
                )
            val viewModel = PersonListScreenModel(fakeRepository)
            assertEquals(ListState.IDLE, viewModel.listState)

            fakeRepository.getPopularPeopleResult =
                Result.success(
                    PersonPageResult(page = 2, list = listOf(Person(id = 2, name = "Person Y")), totalResults = 2, totalPages = 2),
                )
            viewModel.loadPopularPeople()

            assertEquals(
                listOf(Person(id = 1, name = "Person X"), Person(id = 2, name = "Person Y")),
                viewModel.personList,
            )
            assertEquals(listOf(1, 2), fakeRepository.getPopularPeopleCalls)
        }

    @Test
    fun testIOExceptionSetsNetworkErrorState() =
        runTest(testDispatcher) {
            fakeRepository.getPopularPeopleResult = Result.failure(IOException("Mock network failure"))

            val viewModel = PersonListScreenModel(fakeRepository)

            assertEquals(ListState.NETWORK_ERROR, viewModel.listState)
            assertTrue(viewModel.personList.isEmpty())
        }

    @Test
    fun testUnexpectedExceptionSetsErrorState() =
        runTest(testDispatcher) {
            fakeRepository.getPopularPeopleResult = Result.failure(SerializationException("Boom"))

            val viewModel = PersonListScreenModel(fakeRepository)

            assertEquals(ListState.ERROR, viewModel.listState)
        }

    @Test
    fun testHttpExceptionsBadRequestSetsNetworkErrorState() =
        runTest(testDispatcher) {
            fakeRepository.getPopularPeopleResult = Result.failure(badRequestException)

            val viewModel = PersonListScreenModel(fakeRepository)

            assertEquals(ListState.NETWORK_ERROR, viewModel.listState)
        }

    @Test
    fun testHttpExceptionsNotFoundSetsErrorState() =
        runTest(testDispatcher) {
            fakeRepository.getPopularPeopleResult = Result.failure(notFoundException)

            val viewModel = PersonListScreenModel(fakeRepository)

            assertEquals(ListState.ERROR, viewModel.listState)
        }
}
