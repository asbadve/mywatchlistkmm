package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListPageResult
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

private object ListsTabUiTestConstant {
    const val ACCOUNT_ID = 100L
    const val SESSION_ID = "session_abc"
}

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ListsTabUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeListsRepository: FakeListsRepository
    private val session =
        UserSession(
            sessionId = ListsTabUiTestConstant.SESSION_ID,
            accountId = ListsTabUiTestConstant.ACCOUNT_ID,
            username = "jane_doe",
            name = "Jane Doe",
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeListsRepository = FakeListsRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRendersExistingListsAndNewListRow() =
        runComposeUiTest {
            fakeListsRepository.listsResult =
                Result.success(
                    TmdbListPageResult(page = 1, list = listOf(TmdbList(id = 1, name = "Marvel Movies", itemCount = 12)), totalPages = 1),
                )
            val screenModel =
                ListsScreenModel(
                    accountId = ListsTabUiTestConstant.ACCOUNT_ID,
                    sessionId = ListsTabUiTestConstant.SESSION_ID,
                    listsRepository = fakeListsRepository,
                )

            setContent {
                ListsTab(session = session, onListSelected = {}, screenModel = screenModel)
            }

            onNodeWithText("Marvel Movies").assertIsDisplayed()
            onNodeWithText("New list").assertIsDisplayed()
        }

    @Test
    fun testCreatingListInvokesOnListSelected() =
        runComposeUiTest {
            fakeListsRepository.createListResult = Result.success(555L)
            val screenModel =
                ListsScreenModel(
                    accountId = ListsTabUiTestConstant.ACCOUNT_ID,
                    sessionId = ListsTabUiTestConstant.SESSION_ID,
                    listsRepository = fakeListsRepository,
                )
            var selectedListId: Long? = null

            setContent {
                ListsTab(session = session, onListSelected = { selectedListId = it }, screenModel = screenModel)
            }

            onNodeWithText("New list").performClick()
            onNodeWithText("Name").performTextInput("My New List")
            onNodeWithText("Create").performClick()

            assertTrue(selectedListId == 555L)
        }
}
