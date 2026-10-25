package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeCustomListRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.FakeListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.BeforeTest
import kotlin.test.Test

private object ListsTabUiTestConstant {
    const val ACCOUNT_ID = 100L
    const val SESSION_ID = "session_abc"
}

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class ListsTabUiTest {
    private lateinit var fakeListsRepository: FakeListsRepository
    private lateinit var fakeCustomListRepository: FakeCustomListRepository
    private val session =
        UserSession(
            sessionId = ListsTabUiTestConstant.SESSION_ID,
            accountId = ListsTabUiTestConstant.ACCOUNT_ID,
            username = "jane_doe",
            name = "Jane Doe",
        )

    @BeforeTest
    fun setUp() {
        fakeListsRepository = FakeListsRepository()
        fakeCustomListRepository = FakeCustomListRepository()
    }

    @Test
    fun testRendersExistingListsAndNewListRow() =
        runComposeUiTest {
            fakeCustomListRepository.lists = listOf(TmdbList(id = 1, name = "Marvel Movies", itemCount = 12))
            val screenModel =
                ListsScreenModel(
                    accountId = ListsTabUiTestConstant.ACCOUNT_ID,
                    sessionId = ListsTabUiTestConstant.SESSION_ID,
                    listsRepository = fakeListsRepository,
                    customListRepository = fakeCustomListRepository,
                )

            setContent {
                ListsTab(session = session, onListSelected = {}, screenModel = screenModel)
            }
            // The paged list loads on the real main dispatcher - wait for it rather than asserting at once.
            waitUntilExactlyOneExists(hasText("Marvel Movies"))

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
                    customListRepository = fakeCustomListRepository,
                )
            var selectedListId: Long? = null

            setContent {
                ListsTab(session = session, onListSelected = { selectedListId = it }, screenModel = screenModel)
            }

            onNodeWithText("New list").performClick()
            onNodeWithText("Name").performTextInput("My New List")
            onNodeWithText("Create").performClick()

            waitUntil { selectedListId == 555L }
        }
}
