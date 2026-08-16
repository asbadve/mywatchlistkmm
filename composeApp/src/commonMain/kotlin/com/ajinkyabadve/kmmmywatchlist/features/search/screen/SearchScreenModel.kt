package com.ajinkyabadve.kmmmywatchlist.features.search.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchFilter
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import com.ajinkyabadve.kmmmywatchlist.features.search.repository.SearchRepository
import com.ajinkyabadve.kmmmywatchlist.features.search.repository.SearchRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RestrictedModeRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RestrictedModeRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import com.ajinkyabadve.kmmmywatchlist.network.isServerError
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.search_error

class SearchScreenModel(
    private val searchRepository: SearchRepository = SearchRepositoryImpl(),
    private val restrictedModeRepository: RestrictedModeRepository = RestrictedModeRepositoryImpl(),
    private val debounceMillis: Long = SEARCH_DEBOUNCE_MILLIS,
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    /** Raw text field contents - updated on every keystroke so typing stays responsive. */
    var query by mutableStateOf("")
        private set

    var selectedFilter by mutableStateOf(SearchFilter.ALL)
        private set

    var listState by mutableStateOf(ListState.IDLE)
        private set

    var errorMessage by mutableStateOf<UiText?>(null)
        private set

    /** Every loaded result, unfiltered - the chip row narrows this without refetching. */
    private val allResults = mutableStateListOf<SearchResultItem>()

    val results: List<SearchResultItem>
        get() = allResults.filter { selectedFilter.matches(it.mediaType) }

    /**
     * The query the currently displayed results belong to. Distinct from [query], which changes on
     * every keystroke - this only advances once a search actually fires, so the "no results for X"
     * message can't name a string the user has since typed past.
     */
    var submittedQuery by mutableStateOf("")
        private set

    private val queryInput = MutableStateFlow("")
    private var page = 1
    private var canPaginate = false

    /**
     * Keys already appended, so a result that TMDB returns on more than one page is only rendered
     * once. `/3/search/multi` orders by relevance without a stable tiebreak, so paging genuinely
     * repeats items - "love" returns tv-90970 on both page 1 and a later page. Letting a duplicate
     * through crashed the results grid outright ("Key ... was already used" from LazyGrid).
     */
    private val seenKeys = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            queryInput
                .debounce(debounceMillis)
                .map { it.trim() }
                .distinctUntilChanged()
                // collectLatest cancels a still-running request when the debounced query changes
                // again, so a slow response for an older query can never overwrite a newer one.
                .collectLatest { trimmed -> startNewSearch(trimmed) }
        }
    }

    fun onQueryChange(newQuery: String) {
        query = newQuery
        queryInput.value = newQuery
    }

    fun clearQuery() {
        onQueryChange("")
    }

    fun onFilterSelected(filter: SearchFilter) {
        selectedFilter = filter
    }

    /**
     * Called by the grid as it nears the end of the loaded results. No-ops unless there's a live
     * query, another page to fetch, and no request already in flight.
     */
    fun loadNextPage() {
        if (submittedQuery.isBlank() || !canPaginate) return
        if (listState != ListState.IDLE && listState != ListState.NETWORK_ERROR) return
        viewModelScope.launch { fetchPage(isFirstPage = false) }
    }

    fun retry() {
        val pending = submittedQuery
        if (pending.isBlank()) return
        viewModelScope.launch { startNewSearch(pending) }
    }

    private suspend fun startNewSearch(trimmed: String) {
        submittedQuery = trimmed
        page = 1
        canPaginate = false
        allResults.clear()
        seenKeys.clear()
        errorMessage = null

        if (trimmed.isBlank()) {
            listState = ListState.IDLE
            return
        }
        fetchPage(isFirstPage = true)
    }

    private suspend fun fetchPage(isFirstPage: Boolean) {
        listState = if (isFirstPage) ListState.LOADING else ListState.PAGINATING
        try {
            val response =
                searchRepository.searchMulti(
                    submittedQuery,
                    page,
                    includeAdult = !restrictedModeRepository.isRestrictedModeEnabled(),
                )
            // Results whose media_type isn't one of movie/tv/person have no detail screen to open,
            // so they're dropped rather than rendered as dead cards. `seenKeys.add` returns false
            // for a key already present, which drops TMDB's cross-page repeats in the same pass.
            allResults.addAll(
                response.list
                    .orEmpty()
                    .filter { it.mediaType != null && seenKeys.add(it.uniqueKey) },
            )
            canPaginate = response.page < (response.totalPages ?: 0)
            listState =
                if (canPaginate) {
                    page++
                    ListState.IDLE
                } else {
                    ListState.PAGINATION_EXHAUST
                }
        } catch (httpExceptions: HttpExceptions) {
            errorMessage =
                if (httpExceptions.isServerError()) {
                    UiText.Resource(Res.string.error_network)
                } else {
                    UiText.Resource(Res.string.search_error)
                }
            listState =
                if (httpExceptions.isServerError()) {
                    ListState.NETWORK_ERROR
                } else {
                    ListState.ERROR
                }
        } catch (e: IOException) {
            errorMessage = UiText.Resource(Res.string.error_network)
            listState = ListState.NETWORK_ERROR
        } catch (e: ContentConvertException) {
            errorMessage = UiText.Resource(Res.string.search_error)
            listState = ListState.ERROR
        } catch (e: SerializationException) {
            errorMessage = UiText.Resource(Res.string.search_error)
            listState = ListState.ERROR
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    companion object {
        /**
         * Wait this long after the last keystroke before hitting the API. Long enough that typing a
         * word doesn't fire a request per character, short enough that the results feel live.
         */
        const val SEARCH_DEBOUNCE_MILLIS = 350L
    }
}
