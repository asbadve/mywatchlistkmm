package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
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
import kotlin.test.assertTrue

private object ListDetailScreenModelTestConstant {
    const val LIST_ID = 5861L
    const val SESSION_ID = "session_abc"
}

@OptIn(ExperimentalCoroutinesApi::class)
class ListDetailScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeListsRepository = FakeListsRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildModel() =
        ListDetailScreenModel(
            listId = ListDetailScreenModelTestConstant.LIST_ID,
            sessionId = ListDetailScreenModelTestConstant.SESSION_ID,
            listsRepository = fakeListsRepository,
        )

    @Test
    fun testInitialLoadPopulatesItems() =
        runTest(testDispatcher) {
            fakeListsRepository.listDetailsResult =
                Result.success(TmdbListDetail(name = "My List", items = listOf(Movie(id = 1, title = "Movie A"))))

            val viewModel = buildModel()

            val state = viewModel.uiState
            assertTrue(state is ListDetailState.Success)
            assertEquals("My List", state.detail.name)
            assertEquals(1, state.detail.items.size)
        }

    @Test
    fun testRemoveMovieRemovesItemFromState() =
        runTest(testDispatcher) {
            fakeListsRepository.listDetailsResult =
                Result.success(
                    TmdbListDetail(
                        name = "My List",
                        items = listOf(Movie(id = 1, title = "Movie A"), Movie(id = 2, title = "Movie B")),
                    ),
                )
            val viewModel = buildModel()

            viewModel.removeMovie(1L)

            val state = viewModel.uiState
            assertTrue(state is ListDetailState.Success)
            assertEquals(1, state.detail.items.size)
            assertEquals(
                "Movie B",
                state.detail.items
                    .first()
                    .title,
            )
            assertEquals(listOf(1L), fakeListsRepository.removeMovieFromListCalls)
        }

    @Test
    fun testDeleteListSetsIsDeleted() =
        runTest(testDispatcher) {
            fakeListsRepository.listDetailsResult = Result.success(TmdbListDetail(name = "My List"))
            val viewModel = buildModel()

            viewModel.deleteList()

            assertTrue(viewModel.isDeleted)
            assertTrue(fakeListsRepository.deleteListCalled)
        }

    @Test
    fun testNetworkErrorSetsErrorState() =
        runTest(testDispatcher) {
            fakeListsRepository.listDetailsResult = Result.failure(IOException("Mock network failure"))

            val viewModel = buildModel()

            assertTrue(viewModel.uiState is ListDetailState.Error)
        }
}
