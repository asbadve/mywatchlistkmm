package com.ajinkyabadve.kmmmywatchlist.features.person.screen.category

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.FakePersonRepository
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class CommonPersonListScreenContentUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPersonListScreenContent_personClick_invokesOnPersonSelected() =
        runComposeUiTest {
            val fakeRepository =
                FakePersonRepository().apply {
                    getPopularPeopleResult =
                        Result.success(
                            PersonPageResult(
                                page = 1,
                                list = listOf(Person(id = 301, name = "Popular Person")),
                                totalResults = 1,
                                totalPages = 1,
                            ),
                        )
                }
            val viewModel = PersonListScreenModel(fakeRepository)
            var selectedPersonId: Long? = null

            setContent {
                personListScreenContent(viewModel = viewModel, onPersonSelected = { selectedPersonId = it })
            }

            onAllNodesWithText("Popular Person")[0].performClick()
            assertEquals(301L, selectedPersonId)
        }

    @Test
    fun testPersonListScreenContent_networkError_showsRetryThatReloads() =
        runComposeUiTest {
            val fakeRepository =
                FakePersonRepository().apply {
                    getPopularPeopleResult = Result.failure(IOException("boom"))
                }
            val viewModel = PersonListScreenModel(fakeRepository)

            setContent {
                personListScreenContent(viewModel = viewModel)
            }

            onNodeWithText("Retry").assertExists()

            fakeRepository.getPopularPeopleResult =
                Result.success(
                    PersonPageResult(
                        page = 1,
                        list = listOf(Person(id = 302, name = "Recovered Person")),
                        totalResults = 1,
                        totalPages = 1,
                    ),
                )
            onNodeWithText("Retry").performClick()

            onAllNodesWithText("Recovered Person")[0].assertExists()
            assertTrue(fakeRepository.getPopularPeopleCalls.size >= 2)
        }
}
