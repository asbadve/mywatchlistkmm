package com.ajinkyabadve.kmmmywatchlist.features.search.screen

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextRange
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class SearchScreenUiTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val mixedResults =
        SearchPageResult(
            page = 1,
            list =
                listOf(
                    SearchResultItem(id = 603, mediaTypeRaw = "movie", title = "The Matrix", releaseDate = "1999-03-30"),
                    SearchResultItem(id = 12345, mediaTypeRaw = "tv", name = "Matrix Chronicles"),
                    SearchResultItem(id = 6384, mediaTypeRaw = "person", name = "Keanu Reeves"),
                ),
            totalResults = 3,
            // totalPages = 1 with page = 1 marks pagination exhausted immediately, so the grid's
            // near-end-of-list trigger can't loop on this tiny fixture.
            totalPages = 1,
        )

    /**
     * `debounceMillis = 0` keeps these tests about rendering and clicks rather than timing - the
     * debounce itself is covered with virtual time in [SearchScreenModelTest].
     */
    private fun modelWith(result: SearchPageResult): SearchScreenModel {
        val repository = FakeSearchRepository().apply { searchMultiResult = Result.success(result) }
        return SearchScreenModel(repository, debounceMillis = 0L)
    }

    @Test
    fun testEmptyStatePromptsBeforeAnythingIsTyped() =
        runComposeUiTest {
            setContent { SearchScreen(viewModel = modelWith(mixedResults)) }

            onNodeWithText("Start typing to search movies, TV shows and people.").assertIsDisplayed()
        }

    @Test
    fun testTypingRendersResultsWithAMediaTypeBadgePerType() =
        runComposeUiTest {
            val viewModel = modelWith(mixedResults)
            setContent { SearchScreen(viewModel = viewModel) }

            onNodeWithText("Search movies, TV shows, people").performTextInput("matrix")
            waitForIdle()

            onAllNodesWithText("The Matrix")[0].assertIsDisplayed()
            onAllNodesWithText("Matrix Chronicles")[0].assertIsDisplayed()
            onAllNodesWithText("Keanu Reeves")[0].assertIsDisplayed()

            // The differentiator: one badge per media type, rendered over the card.
            onAllNodesWithText("Movie")[0].assertIsDisplayed()
            onAllNodesWithText("Person")[0].assertIsDisplayed()
        }

    @Test
    fun testClickingEachMediaTypeRoutesToItsOwnCallback() =
        runComposeUiTest {
            val viewModel = modelWith(mixedResults)
            var movieId: Long? = null
            var tvShowId: Long? = null
            var personId: Long? = null

            setContent {
                SearchScreen(
                    viewModel = viewModel,
                    onMovieSelected = { movieId = it },
                    onTvShowSelected = { tvShowId = it },
                    onPersonSelected = { personId = it },
                )
            }

            onNodeWithText("Search movies, TV shows, people").performTextInput("matrix")
            waitForIdle()

            onAllNodesWithText("The Matrix")[0].performClick()
            assertEquals(603L, movieId)
            assertNull(tvShowId, "A movie card must not fire the TV callback")

            onAllNodesWithText("Matrix Chronicles")[0].performClick()
            assertEquals(12345L, tvShowId)

            onAllNodesWithText("Keanu Reeves")[0].performClick()
            assertEquals(6384L, personId)
        }

    @Test
    fun testFilterChipNarrowsResultsToOneMediaType() =
        runComposeUiTest {
            val viewModel = modelWith(mixedResults)
            setContent { SearchScreen(viewModel = viewModel) }

            onNodeWithText("Search movies, TV shows, people").performTextInput("matrix")
            waitForIdle()
            assertEquals(3, viewModel.results.size)

            onNodeWithText("People").performClick()
            waitForIdle()

            assertEquals(listOf("Keanu Reeves"), viewModel.results.map { it.displayTitle })
            onAllNodesWithText("Keanu Reeves")[0].assertIsDisplayed()
        }

    @Test
    fun testClearButtonEmptiesTheFieldAndResults() =
        runComposeUiTest {
            val viewModel = modelWith(mixedResults)
            setContent { SearchScreen(viewModel = viewModel) }

            onNodeWithText("Search movies, TV shows, people").performTextInput("matrix")
            waitForIdle()
            assertEquals(3, viewModel.results.size)

            onNodeWithContentDescription("Clear search").performClick()
            waitForIdle()

            assertEquals("", viewModel.query)
            assertEquals(0, viewModel.results.size)
            onNodeWithText("Start typing to search movies, TV shows and people.").assertIsDisplayed()
        }

    /**
     * Regression: the model outlives the composable (it sits in the app-wide ViewModelStore), so
     * reopening search restores the previous query. A String-valued TextField renders that restored
     * text with the selection collapsed at index 0, which made the next keystroke prepend -
     * "matrix" then typing "love" produced "lovematrix" on a real device.
     */
    @Test
    fun testReopeningWithAnExistingQueryPutsTheCaretAtTheEnd() =
        runComposeUiTest {
            val viewModel = modelWith(mixedResults)
            viewModel.onQueryChange("matrix")

            setContent { SearchScreen(viewModel = viewModel) }
            waitForIdle()

            val selection =
                onNodeWithText("matrix")
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.TextSelectionRange]
            assertEquals(TextRange("matrix".length), selection, "Caret should sit after the last character")
        }

    @Test
    fun testTypingIntoARestoredQueryAppendsRatherThanPrepends() =
        runComposeUiTest {
            val viewModel = modelWith(mixedResults)
            viewModel.onQueryChange("matrix")

            setContent { SearchScreen(viewModel = viewModel) }
            waitForIdle()

            onNodeWithText("matrix").performTextInput(" reloaded")
            waitForIdle()

            assertEquals("matrix reloaded", viewModel.query)
        }

    @Test
    fun testBackButtonInvokesCallback() =
        runComposeUiTest {
            var backClicked = false
            setContent {
                SearchScreen(viewModel = modelWith(mixedResults), onBackClicked = { backClicked = true })
            }

            onNodeWithContentDescription("Back").performClick()

            assertEquals(true, backClicked)
        }

    @Test
    fun testEmptyResultSetShowsNoResultsMessageNamingTheQuery() =
        runComposeUiTest {
            val viewModel =
                modelWith(SearchPageResult(page = 1, list = emptyList(), totalResults = 0, totalPages = 0))
            setContent { SearchScreen(viewModel = viewModel) }

            onNodeWithText("Search movies, TV shows, people").performTextInput("zzzzzzzz")
            waitForIdle()

            onNodeWithText("No results for “zzzzzzzz”.").assertIsDisplayed()
        }
}
