package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.DiscoverFilterRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.DiscoverFilterRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.DiscoverRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.DiscoverRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.GenreRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.GenreRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RestrictedModeRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RestrictedModeRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import com.ajinkyabadve.kmmmywatchlist.network.isServerError
import io.github.aakira.napier.log
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

/** TV mirror of [com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.DiscoverMovieScreenModel]. */
class DiscoverTvScreenModel(
    private val discoverRepository: DiscoverRepository = DiscoverRepositoryImpl(),
    private val genreRepository: GenreRepository = GenreRepositoryImpl(),
    private val discoverFilterRepository: DiscoverFilterRepository = DiscoverFilterRepositoryImpl(),
    private val restrictedModeRepository: RestrictedModeRepository = RestrictedModeRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    internal val tvList = mutableStateListOf<Tv>()

    private var page by mutableStateOf(1)
    private var canPaginate by mutableStateOf(false)
    var listState by mutableStateOf(ListState.IDLE)
    var filters by mutableStateOf(discoverFilterRepository.getSelectedTvFilters())
        private set
    var genres by mutableStateOf<List<Genre>>(emptyList())
        private set

    init {
        loadTvShows()
        loadGenres()
    }

    private fun loadGenres() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                genres = genreRepository.getTvGenres()
            } catch (e: HttpExceptions) {
                log { "Http error loading TV genres: ${e.message}" }
            } catch (e: IOException) {
                log { "Network error loading TV genres" }
            }
        }
    }

    fun applyFilters(newFilters: DiscoverFilters) {
        discoverFilterRepository.setSelectedTvFilters(newFilters)
        filters = newFilters
        page = 1
        canPaginate = false
        tvList.clear()
        listState = ListState.IDLE
        loadTvShows()
    }

    internal fun loadTvShows() {
        viewModelScope.launch(Dispatchers.Main) {
            if (isFirstPage() || isNotFirstPageAndCanPaginate() && (isListStateLoadable())) {
                listState = if (isFirstPage()) ListState.LOADING else ListState.PAGINATING

                try {
                    val includeAdult = !restrictedModeRepository.isRestrictedModeEnabled()
                    val response = discoverRepository.getDiscoverTvShows(page, filters, includeAdult)
                    response.list?.let {
                        canPaginate = response.page <= (response.totalPages ?: 0)
                        if (isFirstPage()) {
                            tvList.clear()
                            tvList.addAll(it)
                        } else {
                            tvList.addAll(it)
                        }
                    }
                    listState =
                        if (canPaginate) {
                            page++
                            ListState.IDLE
                        } else {
                            ListState.PAGINATION_EXHAUST
                        }
                } catch (httpExceptions: HttpExceptions) {
                    listState =
                        if (httpExceptions.isServerError()) {
                            ListState.NETWORK_ERROR
                        } else {
                            ListState.ERROR
                        }
                } catch (e: IOException) {
                    log { "IOException" }
                    listState = ListState.NETWORK_ERROR
                } catch (e: ContentConvertException) {
                    log { "Malformed response: ${e.message}" }
                    listState = ListState.ERROR
                } catch (e: SerializationException) {
                    log { "Malformed response: ${e.message}" }
                    listState = ListState.ERROR
                }
            }
        }
    }

    private fun isListStateLoadable() = isListStateIdle() || isListStateNetworkError()

    private fun isNotFirstPageAndCanPaginate() = (page != 1 && canPaginate)

    private fun isListStateIdle() = listState == ListState.IDLE

    private fun isListStateNetworkError() = listState == ListState.NETWORK_ERROR

    private fun isFirstPage() = page == 1

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }
}
