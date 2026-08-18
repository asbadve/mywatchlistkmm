package com.ajinkyabadve.kmmmywatchlist.features.discover.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Keyword
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.KeywordRepository
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
class DiscoverFilterDialogUiTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val noopKeywordRepository =
        object : KeywordRepository {
            override suspend fun searchKeywords(query: String): List<Keyword> = emptyList()
        }
    private val genres = listOf(Genre(id = 28, name = "Action"), Genre(id = 35, name = "Comedy"))

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testReopeningShowsPreviouslyAppliedGenreAndKeywordAsRemovableChips() =
        runComposeUiTest {
            val applied =
                DiscoverFilters(genreIds = setOf(28), keywords = listOf(Keyword(id = 9715, name = "superhero")))

            setContent {
                DiscoverFilterDialog(
                    initialFilters = applied,
                    genres = genres,
                    sortOptions = listOf("popularity.desc"),
                    onDismiss = {},
                    onApply = {},
                    keywordRepository = noopKeywordRepository,
                )
            }

            // The genre chip shows selected (checkmark leading icon renders alongside the label).
            onNodeWithText("Action").assertExists()
            // The previously applied keyword is shown as its own chip - the bug this fixes is that
            // only the id was carried forward before, so the name (and thus a chip to remove it)
            // had nothing to render.
            onNodeWithText("superhero").assertExists()
        }

    @Test
    fun testRemovingAKeywordChipDropsOnlyThatKeywordOnApply() =
        runComposeUiTest {
            val applied =
                DiscoverFilters(
                    keywords = listOf(Keyword(id = 1, name = "one"), Keyword(id = 2, name = "two")),
                )
            var appliedFilters: DiscoverFilters? = null

            setContent {
                DiscoverFilterDialog(
                    initialFilters = applied,
                    genres = genres,
                    sortOptions = listOf("popularity.desc"),
                    onDismiss = {},
                    onApply = { appliedFilters = it },
                    keywordRepository = noopKeywordRepository,
                )
            }

            onNodeWithText("one").performClick()
            onNodeWithText("Apply").performClick()

            assertEquals(listOf(Keyword(id = 2, name = "two")), appliedFilters?.keywords)
        }

    @Test
    fun testClearButtonRemovesAllSelectedGenres() =
        runComposeUiTest {
            val applied = DiscoverFilters(genreIds = setOf(28, 35))
            var appliedFilters: DiscoverFilters? = null

            setContent {
                DiscoverFilterDialog(
                    initialFilters = applied,
                    genres = genres,
                    sortOptions = listOf("popularity.desc"),
                    onDismiss = {},
                    onApply = { appliedFilters = it },
                    keywordRepository = noopKeywordRepository,
                )
            }

            onAllNodesWithText("Clear")[0].performClick()
            onNodeWithText("Apply").performClick()

            assertTrue(appliedFilters?.genreIds.isNullOrEmpty())
        }

    @Test
    fun testClearButtonRemovesAllSelectedKeywords() =
        runComposeUiTest {
            val applied = DiscoverFilters(keywords = listOf(Keyword(id = 1, name = "one")))
            var appliedFilters: DiscoverFilters? = null

            setContent {
                DiscoverFilterDialog(
                    initialFilters = applied,
                    genres = genres,
                    sortOptions = listOf("popularity.desc"),
                    onDismiss = {},
                    onApply = { appliedFilters = it },
                    keywordRepository = noopKeywordRepository,
                )
            }

            // Only the Keywords section has anything to clear here (no genres selected), so the
            // single "Clear" button on screen belongs to it.
            onNodeWithText("Clear").performClick()
            onNodeWithText("Apply").performClick()

            assertTrue(appliedFilters?.keywords.isNullOrEmpty())
        }
}
