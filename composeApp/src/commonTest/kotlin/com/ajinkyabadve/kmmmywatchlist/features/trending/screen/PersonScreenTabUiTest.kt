package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonPageResult
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.FakePersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.viewmodel.PersonListScreenModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PersonScreenTabUiTest {
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
