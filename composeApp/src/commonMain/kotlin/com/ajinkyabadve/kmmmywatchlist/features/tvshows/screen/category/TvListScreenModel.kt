package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import com.ajinkyabadve.kmmmywatchlist.network.isServerError
import io.github.aakira.napier.log
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TvListScreenModel(
    private val tvFetchType: String,
    private val tvRepository: TvRepository = TvRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    internal val tvList = mutableStateListOf<Tv>()

    private var page by mutableStateOf(1)
    private var canPaginate by mutableStateOf(false)
    var listState by mutableStateOf(ListState.IDLE)

    init {
        loadTvShows()
    }

    @Suppress("detekt:TooGenericExceptionCaught")
    internal fun loadTvShows() {
        viewModelScope.launch(Dispatchers.Main) {
            if (isFirstPage() || isNotFirstPageAndCanPaginate() && (isListStateLoadable())) {
                listState = if (isFirstPage()) ListState.LOADING else ListState.PAGINATING

                try {
                    val response = tvRepository.getTvShows(page, tvFetchType)
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
                    httpExceptions.printStackTrace()
                    listState =
                        if (httpExceptions.isServerError()) {
                            ListState.NETWORK_ERROR
                        } else {
                            ListState.ERROR
                        }
                } catch (e: IOException) {
                    e.printStackTrace()
                    log { "IOException" }
                    listState = ListState.NETWORK_ERROR
                } catch (e: Exception) {
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
