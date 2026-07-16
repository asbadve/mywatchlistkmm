package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.SeasonSummary
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

sealed interface AllSeasonsState {
    data object Loading : AllSeasonsState
    data class Success(val seasons: List<SeasonSummary>) : AllSeasonsState
    data class Error(val message: String) : AllSeasonsState
}

class AllSeasonsScreenModel(
    private val tvId: Long,
    private val tvRepository: TvRepository = TvRepositoryImpl()
) : ViewModel() {

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<AllSeasonsState>(AllSeasonsState.Loading)
    val uiState: StateFlow<AllSeasonsState> = _uiState.asStateFlow()

    init {
        loadSeasons()
    }

    @Suppress("detekt:TooGenericExceptionCaught")
    fun loadSeasons() {
        _uiState.value = AllSeasonsState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val detail = tvRepository.getTvDetails(tvId)
                _uiState.value = AllSeasonsState.Success(detail.seasons ?: emptyList())
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = "AllSeasonsScreenModel", throwable = httpExceptions) { "HTTP Error fetching seasons for tvId: $tvId" }
                _uiState.value = AllSeasonsState.Error(httpExceptions.message)
            } catch (e: IOException) {
                Napier.e(tag = "AllSeasonsScreenModel", throwable = e) { "IO/Network Error fetching seasons for tvId: $tvId" }
                _uiState.value = AllSeasonsState.Error("Network Connection Error. Please check your internet connectivity.")
            } catch (e: Exception) {
                Napier.e(tag = "AllSeasonsScreenModel", throwable = e) { "Unexpected Error fetching seasons for tvId: $tvId" }
                _uiState.value = AllSeasonsState.Error("An unexpected error occurred while loading seasons. Please try again.")
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }
}
