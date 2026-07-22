package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.SeasonSummary
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_seasons

sealed interface AllSeasonsState {
    data object Loading : AllSeasonsState

    data class Success(
        val seasons: List<SeasonSummary>,
    ) : AllSeasonsState

    data class Error(
        val message: UiText,
    ) : AllSeasonsState
}

class AllSeasonsScreenModel(
    private val tvId: Long,
    private val tvRepository: TvRepository = TvRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<AllSeasonsState>(AllSeasonsState.Loading)
    val uiState: StateFlow<AllSeasonsState> = _uiState.asStateFlow()

    init {
        loadSeasons()
    }

    fun loadSeasons() {
        _uiState.value = AllSeasonsState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val detail = tvRepository.getTvDetails(tvId)
                _uiState.value = AllSeasonsState.Success(detail.seasons ?: emptyList())
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = TAG, throwable = httpExceptions) { "HTTP Error fetching seasons for tvId: $tvId" }
                _uiState.value = AllSeasonsState.Error(UiText.Plain(httpExceptions.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "IO/Network Error fetching seasons for tvId: $tvId" }
                _uiState.value = AllSeasonsState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: ContentConvertException) {
                logMalformedResponse(e)
                _uiState.value = AllSeasonsState.Error(UiText.Resource(Res.string.error_unexpected_seasons))
            } catch (e: SerializationException) {
                logMalformedResponse(e)
                _uiState.value = AllSeasonsState.Error(UiText.Resource(Res.string.error_unexpected_seasons))
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private fun logMalformedResponse(throwable: Throwable) {
        Napier.e(tag = TAG, throwable = throwable) {
            "Malformed response while loading seasons"
        }
    }

    private companion object {
        const val TAG = "AllSeasonsScreenModel"
    }
}
