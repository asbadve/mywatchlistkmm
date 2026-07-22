package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_tv_details

sealed interface TvDetailState {
    data object Loading : TvDetailState

    data class Success(
        val tvDetail: TvDetail,
        val currentSeason: TvSeasonDetail?,
        val allSeasonDetails: Map<Int, TvSeasonDetail>,
    ) : TvDetailState

    data class Error(
        val message: UiText,
    ) : TvDetailState
}

class TvDetailScreenModel(
    private val tvId: Long,
    private val tvRepository: TvRepository = TvRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<TvDetailState>(TvDetailState.Loading)
    val uiState: StateFlow<TvDetailState> = _uiState.asStateFlow()

    init {
        loadTvDetails()
    }

    fun loadTvDetails() {
        _uiState.value = TvDetailState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val detail = tvRepository.getTvDetails(tvId)
                val seasonDetails = fetchAllSeasonDetails(detail)
                val currentSeasonNumber = resolveCurrentSeasonNumber(detail)
                _uiState.value =
                    TvDetailState.Success(
                        tvDetail = detail,
                        currentSeason = seasonDetails[currentSeasonNumber],
                        allSeasonDetails = seasonDetails,
                    )
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = TAG, throwable = httpExceptions) { "HTTP Error fetching details for tvId: $tvId" }
                _uiState.value = TvDetailState.Error(UiText.Plain(httpExceptions.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "IO/Network Error fetching details for tvId: $tvId" }
                _uiState.value = TvDetailState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: ContentConvertException) {
                logMalformedResponse(e)
                _uiState.value = TvDetailState.Error(UiText.Resource(Res.string.error_unexpected_tv_details))
            } catch (e: SerializationException) {
                logMalformedResponse(e)
                _uiState.value = TvDetailState.Error(UiText.Resource(Res.string.error_unexpected_tv_details))
            }
        }
    }

    private suspend fun fetchAllSeasonDetails(detail: TvDetail): Map<Int, TvSeasonDetail> {
        val seasonNumbers = detail.seasons?.map { it.seasonNumber }?.filter { it >= 0 } ?: emptyList()
        return coroutineScope {
            seasonNumbers
                .map { seasonNumber ->
                    async { fetchSeasonDetailOrNull(seasonNumber) }
                }.awaitAll()
        }.filterNotNull().associateBy { it.seasonNumber }
    }

    private suspend fun fetchSeasonDetailOrNull(seasonNumber: Int): TvSeasonDetail? =
        try {
            tvRepository.getSeasonDetails(tvId, seasonNumber)
        } catch (e: HttpExceptions) {
            logSeasonFailure(seasonNumber, e)
            null
        } catch (e: IOException) {
            logSeasonFailure(seasonNumber, e)
            null
        } catch (e: ContentConvertException) {
            logSeasonFailure(seasonNumber, e)
            null
        } catch (e: SerializationException) {
            logSeasonFailure(seasonNumber, e)
            null
        }

    private fun logSeasonFailure(
        seasonNumber: Int,
        throwable: Throwable,
    ) {
        Napier.e(tag = TAG, throwable = throwable) { "Failed to load season $seasonNumber for tvId: $tvId" }
    }

    private fun resolveCurrentSeasonNumber(detail: TvDetail): Int? {
        detail.nextEpisodeToAir?.let { return it.seasonNumber }
        detail.lastEpisodeToAir?.let { return it.seasonNumber }
        return detail.seasons
            ?.filter { it.seasonNumber > 0 }
            ?.maxByOrNull { it.seasonNumber }
            ?.seasonNumber
            ?: detail.seasons?.maxByOrNull { it.seasonNumber }?.seasonNumber
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private fun logMalformedResponse(throwable: Throwable) {
        Napier.e(tag = TAG, throwable = throwable) {
            "Malformed response while loading tv show details"
        }
    }

    private companion object {
        const val TAG = "TvDetailScreenModel"
    }
}
