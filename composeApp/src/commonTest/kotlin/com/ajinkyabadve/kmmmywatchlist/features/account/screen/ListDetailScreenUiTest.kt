package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

private object ListDetailScreenUiTestConstant {
    const val LIST_ID = 5861L
    const val SESSION_ID = "session_abc"
}

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ListDetailScreenUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeListsRepository: FakeListsRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeListsRepository = FakeListsRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildModel() =
        ListDetailScreenModel(
            listId = ListDetailScreenUiTestConstant.LIST_ID,
            sessionId = ListDetailScreenUiTestConstant.SESSION_ID,
            listsRepository = fakeListsRepository,
        )

    @Test
    fun testRendersListNameAndItems() =
        runComposeUiTest {
            fakeListsRepository.listDetailsResult =
                Result.success(TmdbListDetail(name = "Marvel Movies", items = listOf(Movie(id = 1, title = "Iron Man"))))
            val viewModel = buildModel()

            setContent {
                ListDetailScreen(
                    listId = ListDetailScreenUiTestConstant.LIST_ID,
                    sessionId = ListDetailScreenUiTestConstant.SESSION_ID,
                    onBackClicked = {},
                    onMovieClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithText("Marvel Movies").assertIsDisplayed()
            onNodeWithText("Iron Man").assertIsDisplayed()
        }

    @Test
    fun testRemovingItemDropsItFromTheList() =
        runComposeUiTest {
            fakeListsRepository.listDetailsResult =
                Result.success(
                    TmdbListDetail(name = "Marvel Movies", items = listOf(Movie(id = 1, title = "Iron Man"))),
                )
            val viewModel = buildModel()

            setContent {
                ListDetailScreen(
                    listId = ListDetailScreenUiTestConstant.LIST_ID,
                    sessionId = ListDetailScreenUiTestConstant.SESSION_ID,
                    onBackClicked = {},
                    onMovieClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithContentDescription("Remove from list").performClick()

            assertTrue(fakeListsRepository.removeMovieFromListCalls.contains(1L))
        }

    @Test
    fun testDeletingListInvokesOnBackClicked() =
        runComposeUiTest {
            fakeListsRepository.listDetailsResult = Result.success(TmdbListDetail(name = "Marvel Movies"))
            val viewModel = buildModel()
            var backClicked = false

            setContent {
                ListDetailScreen(
                    listId = ListDetailScreenUiTestConstant.LIST_ID,
                    sessionId = ListDetailScreenUiTestConstant.SESSION_ID,
                    onBackClicked = { backClicked = true },
                    onMovieClicked = {},
                    viewModel = viewModel,
                )
            }

            onNodeWithContentDescription("Delete list").performClick()
            onNodeWithText("Delete").performClick()
            waitForIdle()

            assertTrue(backClicked)
            assertTrue(fakeListsRepository.deleteListCalled)
        }
}
