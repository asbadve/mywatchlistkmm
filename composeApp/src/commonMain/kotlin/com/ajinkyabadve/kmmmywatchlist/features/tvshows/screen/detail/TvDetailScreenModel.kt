package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
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

sealed interface TvDetailState {
    data object Loading : TvDetailState
    data class Success(
        val tvDetail: TvDetail,
        val currentSeason: TvSeasonDetail?,
        val allSeasonDetails: Map<Int, TvSeasonDetail>,
    ) : TvDetailState
    data class Error(val message: String) : TvDetailState
}

class TvDetailScreenModel(
    private val tvId: Long,
    private val tvRepository: TvRepository = TvRepositoryImpl()
) : ViewModel() {

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<TvDetailState>(TvDetailState.Loading)
    val uiState: StateFlow<TvDetailState> = _uiState.asStateFlow()

    init {
        loadTvDetails()
    }

    @Suppress("detekt:TooGenericExceptionCaught")
    fun loadTvDetails() {
        _uiState.value = TvDetailState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val detail = tvRepository.getTvDetails(tvId)
                val seasonDetails = fetchAllSeasonDetails(detail)
                val currentSeasonNumber = resolveCurrentSeasonNumber(detail)
                _uiState.value = TvDetailState.Success(
                    tvDetail = detail,
                    currentSeason = seasonDetails[currentSeasonNumber],
                    allSeasonDetails = seasonDetails,
                )
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = "TvDetailScreenModel", throwable = httpExceptions) { "HTTP Error fetching details for tvId: $tvId" }
                _uiState.value = TvDetailState.Error(httpExceptions.message)
            } catch (e: IOException) {
                Napier.e(tag = "TvDetailScreenModel", throwable = e) { "IO/Network Error fetching details for tvId: $tvId" }
                _uiState.value = TvDetailState.Error("Network Connection Error. Please check your internet connectivity.")
            } catch (e: Exception) {
                Napier.e(tag = "TvDetailScreenModel", throwable = e) { "Unexpected Error fetching details for tvId: $tvId" }
                _uiState.value = TvDetailState.Error("An unexpected error occurred while loading tv show details. Please try again.")
            }
        }
    }

    private suspend fun fetchAllSeasonDetails(detail: TvDetail): Map<Int, TvSeasonDetail> {
        val seasonNumbers = detail.seasons?.map { it.seasonNumber }?.filter { it >= 0 } ?: emptyList()
        return coroutineScope {
            seasonNumbers.map { seasonNumber ->
                async { fetchSeasonDetailOrNull(seasonNumber) }
            }.awaitAll()
        }.filterNotNull().associateBy { it.seasonNumber }
    }

    @Suppress("detekt:TooGenericExceptionCaught")
    private suspend fun fetchSeasonDetailOrNull(seasonNumber: Int): TvSeasonDetail? {
        return try {
            tvRepository.getSeasonDetails(tvId, seasonNumber)
        } catch (e: Exception) {
            Napier.e(tag = "TvDetailScreenModel", throwable = e) { "Failed to load season $seasonNumber for tvId: $tvId" }
            null
        }
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
}
