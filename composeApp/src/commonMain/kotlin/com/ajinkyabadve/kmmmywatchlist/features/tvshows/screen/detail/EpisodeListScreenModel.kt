package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
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
import mywatchlist.composeapp.generated.resources.error_unexpected_episodes

sealed interface EpisodeListState {
    data object Loading : EpisodeListState

    data class Success(
        val season: TvSeasonDetail,
    ) : EpisodeListState

    data class Error(
        val message: UiText,
    ) : EpisodeListState
}

class EpisodeListScreenModel(
    private val tvId: Long,
    private val seasonNumber: Int,
    private val tvRepository: TvRepository = TvRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<EpisodeListState>(EpisodeListState.Loading)
    val uiState: StateFlow<EpisodeListState> = _uiState.asStateFlow()

    init {
        loadEpisodes()
    }

    fun loadEpisodes() {
        _uiState.value = EpisodeListState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val season = tvRepository.getSeasonDetails(tvId, seasonNumber)
                _uiState.value = EpisodeListState.Success(season)
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = TAG, throwable = httpExceptions) {
                    "HTTP Error fetching season $seasonNumber for tvId: $tvId"
                }
                _uiState.value = EpisodeListState.Error(UiText.Plain(httpExceptions.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) {
                    "IO/Network Error fetching season $seasonNumber for tvId: $tvId"
                }
                _uiState.value = EpisodeListState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: ContentConvertException) {
                logMalformedResponse(e)
                _uiState.value = EpisodeListState.Error(UiText.Resource(Res.string.error_unexpected_episodes))
            } catch (e: SerializationException) {
                logMalformedResponse(e)
                _uiState.value = EpisodeListState.Error(UiText.Resource(Res.string.error_unexpected_episodes))
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private fun logMalformedResponse(throwable: Throwable) {
        Napier.e(tag = TAG, throwable = throwable) {
            "Malformed response while loading episodes"
        }
    }

    private companion object {
        const val TAG = "EpisodeListScreenModel"
    }
}
