package com.ajinkyabadve.kmmmywatchlist.features.movies.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.util.addAllNewBy
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import com.ajinkyabadve.kmmmywatchlist.network.isServerError
import io.github.aakira.napier.Napier
import io.github.aakira.napier.log
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

class MovieListScreenModel(
    private val movieFetchType: String,
    private val movieRepository: MovieRepository = MovieRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    internal val movieList = mutableStateListOf<Movie>()

    private var page by mutableStateOf(1)
    private var canPaginate by mutableStateOf(false)
    var listState by mutableStateOf(ListState.IDLE)

    init {
        loadMovies()
    }

    internal fun loadMovies() {
        viewModelScope.launch(Dispatchers.Main) {
            if (!isLoadInFlightOrExhausted() && (isFirstPage() || isNotFirstPageAndCanPaginate() && isListStateLoadable())) {
                listState = if (isFirstPage()) ListState.LOADING else ListState.PAGINATING

                try {
                    val response = movieRepository.getMovies(page, movieFetchType)
                    response.list?.let {
                        canPaginate = response.page < response.totalPages
                        if (isFirstPage()) movieList.clear()
                        movieList.addAllNewBy(it) { movie -> movie.id }
                    }
                    listState =
                        if (canPaginate) {
                            page++
                            ListState.IDLE
                        } else {
                            ListState.PAGINATION_EXHAUST
                        }
                } catch (httpExceptions: HttpExceptions) {
                    // todo find another exception solution
                    Napier.e(throwable = httpExceptions) { LOG_HTTP_ERROR }
                    listState =
                        if (httpExceptions.isServerError()) {
                            // todo check network error specific and add retry button if its not last one
                            ListState.NETWORK_ERROR
                        } else {
                            ListState.ERROR
                        }
                } catch (e: IOException) {
                    // todo catch //java.net.UnknownHostException: Unable to resolve host
                    //  "api.themoviedb.org": No address associated with hostname
                    Napier.e(throwable = e) { LOG_NETWORK_ERROR }
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

    // init's first load and the screen's pagination effect can both call this before either runs.
    // A first-page load never checked the state, so page 1 was fetched twice - and when page 1 was
    // also the last, `page` stays 1, so the queued second call refetched it after it finished.
    private fun isLoadInFlightOrExhausted() =
        listState == ListState.LOADING || listState == ListState.PAGINATING || listState == ListState.PAGINATION_EXHAUST

    private fun isNotFirstPageAndCanPaginate() = (page != 1 && canPaginate)

    private fun isListStateIdle() = listState == ListState.IDLE

    private fun isListStateNetworkError() = listState == ListState.NETWORK_ERROR

    private fun isFirstPage() = page == 1

    override fun onCleared() {
        page = 1
        listState = ListState.IDLE
        canPaginate = false
        viewModelScope.cancel()
        super.onCleared()
    }

    private companion object {
        // Logged through Napier rather than printStackTrace(): Napier is silent until initLogging()
        // installs a backend, and raw stdout stack traces from a finished coroutine break Gradle's
        // Kotlin/Native test-output parser (failed the iOS CI job on PR #23).
        const val LOG_HTTP_ERROR = "Http error loading list page"
        const val LOG_NETWORK_ERROR = "Network error loading list page"
    }
}
