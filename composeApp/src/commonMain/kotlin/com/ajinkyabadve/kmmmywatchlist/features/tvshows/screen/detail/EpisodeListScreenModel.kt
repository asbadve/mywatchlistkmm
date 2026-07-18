package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EpisodeListState {
    data object Loading : EpisodeListState
    data class Success(val season: TvSeasonDetail) : EpisodeListState
    data class Error(val message: String) : EpisodeListState
}

class EpisodeListScreenModel(
    private val tvId: Long,
    private val seasonNumber: Int,
    private val tvRepository: TvRepository = TvRepositoryImpl()
) : ViewModel() {

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<EpisodeListState>(EpisodeListState.Loading)
    val uiState: StateFlow<EpisodeListState> = _uiState.asStateFlow()

    init {
        loadEpisodes()
    }

    @Suppress("detekt:TooGenericExceptionCaught")
    fun loadEpisodes() {
        _uiState.value = EpisodeListState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val season = tvRepository.getSeasonDetails(tvId, seasonNumber)
                _uiState.value = EpisodeListState.Success(season)
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = "EpisodeListScreenModel", throwable = httpExceptions) {
                    "HTTP Error fetching season $seasonNumber for tvId: $tvId"
                }
                _uiState.value = EpisodeListState.Error(httpExceptions.message)
            } catch (e: IOException) {
                Napier.e(tag = "EpisodeListScreenModel", throwable = e) {
                    "IO/Network Error fetching season $seasonNumber for tvId: $tvId"
                }
                _uiState.value = EpisodeListState.Error("Network Connection Error. Please check your internet connectivity.")
            } catch (e: Exception) {
                Napier.e(tag = "EpisodeListScreenModel", throwable = e) {
                    "Unexpected Error fetching season $seasonNumber for tvId: $tvId"
                }
                _uiState.value = EpisodeListState.Error("An unexpected error occurred while loading episodes. Please try again.")
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }
}
