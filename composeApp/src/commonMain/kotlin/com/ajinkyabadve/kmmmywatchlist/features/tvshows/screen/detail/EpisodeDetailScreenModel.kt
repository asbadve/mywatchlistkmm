package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.EpisodeDetail
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
import mywatchlist.composeapp.generated.resources.error_unexpected_episode

sealed interface EpisodeDetailState {
    data object Loading : EpisodeDetailState

    data class Success(
        val episode: EpisodeDetail,
    ) : EpisodeDetailState

    data class Error(
        val message: UiText,
    ) : EpisodeDetailState
}

class EpisodeDetailScreenModel(
    private val tvId: Long,
    private val seasonNumber: Int,
    private val episodeNumber: Int,
    private val tvRepository: TvRepository = TvRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<EpisodeDetailState>(EpisodeDetailState.Loading)
    val uiState: StateFlow<EpisodeDetailState> = _uiState.asStateFlow()

    init {
        loadEpisodeDetails()
    }

    fun loadEpisodeDetails() {
        _uiState.value = EpisodeDetailState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val episode = tvRepository.getEpisodeDetails(tvId, seasonNumber, episodeNumber)
                _uiState.value = EpisodeDetailState.Success(episode)
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = TAG, throwable = httpExceptions) {
                    "HTTP Error fetching episode $episodeNumber (season $seasonNumber) for tvId: $tvId"
                }
                _uiState.value = EpisodeDetailState.Error(UiText.Plain(httpExceptions.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) {
                    "IO/Network Error fetching episode $episodeNumber (season $seasonNumber) for tvId: $tvId"
                }
                _uiState.value = EpisodeDetailState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: ContentConvertException) {
                logMalformedResponse(e)
                _uiState.value = EpisodeDetailState.Error(UiText.Resource(Res.string.error_unexpected_episode))
            } catch (e: SerializationException) {
                logMalformedResponse(e)
                _uiState.value = EpisodeDetailState.Error(UiText.Resource(Res.string.error_unexpected_episode))
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private fun logMalformedResponse(throwable: Throwable) {
        Napier.e(tag = TAG, throwable = throwable) {
            "Malformed response while loading episode details"
        }
    }

    private companion object {
        const val TAG = "EpisodeDetailScreenModel"
    }
}
