package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.FakePersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.category.PersonListScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PersonScreenTabUiTest {
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
    fun testPersonScreenTab_personClick_invokesOnPersonSelected() =
        runComposeUiTest {
            // totalPages = 0 marks pagination as exhausted after the first load - otherwise the
            // fake's page always reports back as 1, so `canPaginate = response.page <=
            // (response.totalPages ?: 0)` (PersonListScreenModel.kt) stays true forever and, with
            // only one item in a small grid, CommonPersonListScreenContent's near-end-of-list
            // pagination trigger keeps re-firing loadPopularPeople() in an unbounded loop.
            val fakeRepository =
                FakePersonRepository().apply {
                    getPopularPeopleResult =
                        Result.success(
                            PersonPageResult(
                                page = 1,
                                list = listOf(Person(id = 301, name = "Person A")),
                                totalResults = 1,
                                totalPages = 0,
                            ),
                        )
                }
            val viewModel = PersonListScreenModel(fakeRepository)
            var selectedPersonId: Long? = null

            setContent {
                PersonScreenTab(viewModel = viewModel, onPersonSelected = { selectedPersonId = it })
            }

            onAllNodesWithText("Person A")[0].performClick()
            assertEquals(301L, selectedPersonId)
        }
}
