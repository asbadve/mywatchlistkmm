package com.ajinkyabadve.kmmmywatchlist.features.search.screen

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchFilter
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchScreenModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun movie(
        id: Int,
        title: String,
    ) = SearchResultItem(id = id, mediaTypeRaw = "movie", title = title)

    private fun tv(
        id: Int,
        name: String,
    ) = SearchResultItem(id = id, mediaTypeRaw = "tv", name = name)

    private fun person(
        id: Int,
        name: String,
    ) = SearchResultItem(id = id, mediaTypeRaw = "person", name = name)

    private fun page(
        items: List<SearchResultItem>,
        page: Int = 1,
        totalPages: Int = 1,
    ) = SearchPageResult(page = page, list = items, totalResults = items.size, totalPages = totalPages)

    @Test
    fun testTypingDoesNotSearchUntilTheDebounceWindowElapses() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeSearchRepository().apply {
                    searchMultiResult = Result.success(page(listOf(movie(1, "The Matrix"))))
                }
            val viewModel = SearchScreenModel(repository, debounceMillis = DEBOUNCE)

            // Simulate a burst of keystrokes well inside one debounce window.
            viewModel.onQueryChange("m")
            advanceTimeBy(50)
            viewModel.onQueryChange("ma")
            advanceTimeBy(50)
            viewModel.onQueryChange("mat")
            advanceTimeBy(50)

            assertTrue(
                repository.searchMultiCalls.isEmpty(),
                "Expected no request while the user is still typing, got ${repository.searchMultiCalls}",
            )

            advanceUntilIdle()

            assertEquals(
                listOf("mat" to 1),
                repository.searchMultiCalls,
                "Expected exactly one request, for the final text only",
            )
            assertEquals(listOf("The Matrix"), viewModel.results.map { it.displayTitle })
        }

    @Test
    fun testEachSettledQueryIssuesItsOwnRequest() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeSearchRepository().apply {
                    searchMultiResult = Result.success(page(listOf(movie(1, "The Matrix"))))
                }
            val viewModel = SearchScreenModel(repository, debounceMillis = DEBOUNCE)

            viewModel.onQueryChange("matrix")
            advanceUntilIdle()
            viewModel.onQueryChange("matrix reloaded")
            advanceUntilIdle()

            assertEquals(listOf("matrix" to 1, "matrix reloaded" to 1), repository.searchMultiCalls)
        }

    @Test
    fun testQueryIsTrimmedAndRetypingTheSameTextDoesNotRefetch() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeSearchRepository().apply {
                    searchMultiResult = Result.success(page(listOf(movie(1, "The Matrix"))))
                }
            val viewModel = SearchScreenModel(repository, debounceMillis = DEBOUNCE)

            viewModel.onQueryChange("matrix")
            advanceUntilIdle()
            // Trailing whitespace trims to the same query - distinctUntilChanged should swallow it.
            viewModel.onQueryChange("matrix ")
            advanceUntilIdle()

            assertEquals(listOf("matrix" to 1), repository.searchMultiCalls)
        }

    @Test
    fun testBlankQueryClearsResultsWithoutSearching() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeSearchRepository().apply {
                    searchMultiResult = Result.success(page(listOf(movie(1, "The Matrix"))))
                }
            val viewModel = SearchScreenModel(repository, debounceMillis = DEBOUNCE)

            viewModel.onQueryChange("matrix")
            advanceUntilIdle()
            assertEquals(1, viewModel.results.size)

            viewModel.clearQuery()
            advanceUntilIdle()

            assertTrue(viewModel.results.isEmpty())
            assertEquals(ListState.IDLE, viewModel.listState)
            assertEquals("", viewModel.submittedQuery)
            // Still just the one call from the original query - clearing doesn't hit the API.
            assertEquals(listOf("matrix" to 1), repository.searchMultiCalls)
        }

    @Test
    fun testFilterNarrowsResultsByMediaTypeWithoutRefetching() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeSearchRepository().apply {
                    searchMultiResult =
                        Result.success(
                            page(
                                listOf(
                                    movie(1, "The Matrix"),
                                    tv(2, "The Matrix Series"),
                                    person(3, "Keanu Reeves"),
                                ),
                            ),
                        )
                }
            val viewModel = SearchScreenModel(repository, debounceMillis = DEBOUNCE)

            viewModel.onQueryChange("matrix")
            advanceUntilIdle()
            assertEquals(3, viewModel.results.size)

            viewModel.onFilterSelected(SearchFilter.PEOPLE)
            assertEquals(listOf("Keanu Reeves"), viewModel.results.map { it.displayTitle })

            viewModel.onFilterSelected(SearchFilter.TV_SHOWS)
            assertEquals(listOf("The Matrix Series"), viewModel.results.map { it.displayTitle })

            viewModel.onFilterSelected(SearchFilter.MOVIES)
            assertEquals(listOf("The Matrix"), viewModel.results.map { it.displayTitle })

            viewModel.onFilterSelected(SearchFilter.ALL)
            assertEquals(3, viewModel.results.size)

            // Filtering is client-side over the already-loaded page.
            assertEquals(1, repository.searchMultiCalls.size)
        }

    @Test
    fun testUnknownMediaTypesAreDropped() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeSearchRepository().apply {
                    searchMultiResult =
                        Result.success(
                            page(
                                listOf(
                                    movie(1, "The Matrix"),
                                    SearchResultItem(id = 9, mediaTypeRaw = "collection", name = "Matrix Collection"),
                                ),
                            ),
                        )
                }
            val viewModel = SearchScreenModel(repository, debounceMillis = DEBOUNCE)

            viewModel.onQueryChange("matrix")
            advanceUntilIdle()

            assertEquals(listOf("The Matrix"), viewModel.results.map { it.displayTitle })
        }

    @Test
    fun testPaginationAppendsNextPageAndStopsAtTheLastOne() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeSearchRepository().apply {
                    searchMultiResult = Result.success(page(listOf(movie(1, "Page One")), page = 1, totalPages = 2))
                }
            val viewModel = SearchScreenModel(repository, debounceMillis = DEBOUNCE)

            viewModel.onQueryChange("matrix")
            advanceUntilIdle()
            assertEquals(ListState.IDLE, viewModel.listState)

            repository.searchMultiResult =
                Result.success(page(listOf(movie(2, "Page Two")), page = 2, totalPages = 2))
            viewModel.loadNextPage()
            advanceUntilIdle()

            assertEquals(listOf("Page One", "Page Two"), viewModel.results.map { it.displayTitle })
            assertEquals(listOf("matrix" to 1, "matrix" to 2), repository.searchMultiCalls)
            assertEquals(ListState.PAGINATION_EXHAUST, viewModel.listState)

            // Exhausted - further scrolling must not fire more requests.
            viewModel.loadNextPage()
            advanceUntilIdle()
            assertEquals(2, repository.searchMultiCalls.size)
        }

    /**
     * Regression: `/3/search/multi` orders by relevance with no stable tiebreak, so the same item
     * legitimately comes back on more than one page (verified against the live API - "love" returns
     * tv-90970 twice within the first five pages). Appending it twice gave the results grid two
     * items with the same key, which crashed on the next measure pass with
     * `IllegalArgumentException: Key "tv-90970" was already used`.
     */
    @Test
    fun testDuplicateResultsAcrossPagesAreAppendedOnlyOnce() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repeated = tv(90970, "Repeated Show")
            val repository =
                FakeSearchRepository().apply {
                    searchMultiResult =
                        Result.success(page(listOf(movie(1, "Page One"), repeated), page = 1, totalPages = 2))
                }
            val viewModel = SearchScreenModel(repository, debounceMillis = DEBOUNCE)

            viewModel.onQueryChange("love")
            advanceUntilIdle()

            repository.searchMultiResult =
                Result.success(page(listOf(repeated, movie(2, "Page Two")), page = 2, totalPages = 2))
            viewModel.loadNextPage()
            advanceUntilIdle()

            val keys = viewModel.results.map { it.uniqueKey }
            assertEquals(keys.distinct(), keys, "Grid keys must be unique across pages, got $keys")
            assertEquals(
                listOf("Page One", "Repeated Show", "Page Two"),
                viewModel.results.map { it.displayTitle },
                "The repeat should be dropped, not re-appended or replacing the first copy",
            )
        }

    @Test
    fun testNewSearchResetsDeduplicationSoRepeatedQueriesStillReturnResults() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeSearchRepository().apply {
                    searchMultiResult = Result.success(page(listOf(movie(1, "The Matrix"))))
                }
            val viewModel = SearchScreenModel(repository, debounceMillis = DEBOUNCE)

            viewModel.onQueryChange("matrix")
            advanceUntilIdle()
            viewModel.onQueryChange("matrix reloaded")
            advanceUntilIdle()

            // Same item id as the previous query - the seen-key set must not suppress it.
            assertEquals(listOf("The Matrix"), viewModel.results.map { it.displayTitle })
        }

    @Test
    fun testNetworkFailureSurfacesRetryableErrorState() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeSearchRepository().apply {
                    searchMultiResult = Result.failure(IOException("offline"))
                }
            val viewModel = SearchScreenModel(repository, debounceMillis = DEBOUNCE)

            viewModel.onQueryChange("matrix")
            advanceUntilIdle()

            assertEquals(ListState.NETWORK_ERROR, viewModel.listState)
            assertEquals(UiText.Resource(Res.string.error_network), viewModel.errorMessage)

            repository.searchMultiResult = Result.success(page(listOf(movie(1, "The Matrix"))))
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(listOf("The Matrix"), viewModel.results.map { it.displayTitle })
            assertEquals(null, viewModel.errorMessage)
        }

    @Test
    fun testMediaTypeMappingCoversAllThreeSupportedTypes() {
        assertEquals(SearchMediaType.MOVIE, movie(1, "m").mediaType)
        assertEquals(SearchMediaType.TV, tv(2, "t").mediaType)
        assertEquals(SearchMediaType.PERSON, person(3, "p").mediaType)
        assertEquals(null, SearchResultItem(id = 4, mediaTypeRaw = "collection").mediaType)
    }

    private companion object {
        const val DEBOUNCE = 300L
    }
}
