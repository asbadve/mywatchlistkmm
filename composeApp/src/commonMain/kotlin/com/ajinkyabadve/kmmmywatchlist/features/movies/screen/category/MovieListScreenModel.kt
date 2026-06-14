package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import com.ajinkyabadve.kmmmywatchlist.network.isServerError
import io.github.aakira.napier.log
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MovieListScreenModel(
    private val movieFetchType: String,
    private val movieRepository: MovieRepositoryImpl = MovieRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    internal val movieList = mutableStateListOf<Movie>()

    private var page by mutableStateOf(1)
    private var canPaginate by mutableStateOf(false)
    var listState by mutableStateOf(ListState.IDLE)

    init {
        loadMovies()
    }

    @Suppress("detekt:TooGenericExceptionCaught")
    internal fun loadMovies() {
        viewModelScope.launch(Dispatchers.Main) {
            if (isFirstPage() || isNotFirstPageAndCanPaginate() && (isListStateLoadable())) {
                listState = if (isFirstPage()) ListState.LOADING else ListState.PAGINATING

                try {
                    val response = movieRepository.getMovies(page, movieFetchType)
                    response.list?.let {
                        canPaginate = response.page <= response.totalPages
                        if (isFirstPage()) {
                            movieList.clear()
                            movieList.addAll(it)
                        } else {
                            movieList.addAll(it)
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
                    // todo find another exception solution
                    httpExceptions.printStackTrace()
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
                    e.printStackTrace()
                    log { "IOException" }
                    listState = ListState.NETWORK_ERROR
                } catch (e: Exception) {
                    // todo right now there is no way to find out specific exception
                    e.printStackTrace()
                    log { "Exception" }
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
        page = 1
        listState = ListState.IDLE
        canPaginate = false
        viewModelScope.cancel()
        super.onCleared()
    }
}
