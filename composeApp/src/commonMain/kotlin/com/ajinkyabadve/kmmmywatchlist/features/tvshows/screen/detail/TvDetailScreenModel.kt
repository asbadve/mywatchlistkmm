package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.MediaActionsState
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.loadOnSessionAvailable
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.AccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.AccountMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvDetailCacheRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvDetailCacheRepositoryImpl
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_tv_details

sealed interface TvDetailState {
    data object Loading : TvDetailState

    data class Success(
        val tvDetail: TvDetail,
        val currentSeason: TvSeasonDetail?,
        // The latest already-aired episode within [currentSeason], verified against today's date
        // rather than trusted from TMDB's `last_episode_to_air` alone - see resolveCurrentSeason.
        val latestReleasedEpisodeNumber: Int?,
        val allSeasonDetails: Map<Int, TvSeasonDetail>,
        val regionCode: String,
        val fallbackRegionCode: String,
    ) : TvDetailState

    data class Error(
        val message: UiText,
    ) : TvDetailState
}

class TvDetailScreenModel(
    private val tvId: Long,
    private val tvDetailCacheRepository: TvDetailCacheRepository = TvDetailCacheRepositoryImpl(),
    private val regionRepository: RegionRepository = RegionRepositoryImpl(),
    authRepository: AuthRepository = AuthRepositoryImpl(),
    accountMediaRepository: AccountMediaRepository = AccountMediaRepositoryImpl(),
    trackedMediaRepository: TrackedMediaRepository = TrackedMediaRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<TvDetailState>(TvDetailState.Loading)
    val uiState: StateFlow<TvDetailState> = _uiState.asStateFlow()

    /**
     * Owned here, not by `MediaActionButtons` itself - see [MediaActionsState]'s kdoc for why a
     * reusable composable never gets its own `ViewModel`. Launches on this screen's
     * `viewModelScope`, so the toggle survives past whatever recomposes the hero.
     */
    val mediaActionsState =
        MediaActionsState(MediaTypeConstant.TV, tvId, viewModelScope, accountMediaRepository, trackedMediaRepository)

    init {
        loadTvDetails()
        // This ViewModel triggers the `account_states` pre-check, not `MediaActionButtons` - the
        // moment a session appears (already logged in, or logging in while this screen is open),
        // not on some composable's recomposition/LaunchedEffect timing.
        viewModelScope.launch { mediaActionsState.loadOnSessionAvailable(authRepository) }
    }

    fun loadTvDetails() {
        // TvDetailCacheRepository is the single source of truth: it decides cache-vs-network (per
        // show and per season) and writes fresh data through to the DB - this only renders whatever
        // it emits, and separately triggers a refresh. See TvDetailCacheRepository's kdoc.
        viewModelScope.launch(Dispatchers.Main) {
            combine(tvDetailCacheRepository.observe(tvId), tvDetailCacheRepository.observeSeasons(tvId)) { detail, seasons ->
                detail?.let { buildSuccessState(it, seasons) }
            }.collect { state -> if (state != null) _uiState.value = state }
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                tvDetailCacheRepository.refresh(tvId)
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = TAG, throwable = httpExceptions) { "HTTP Error fetching details for tvId: $tvId" }
                if (_uiState.value !is TvDetailState.Success) _uiState.value = TvDetailState.Error(UiText.Plain(httpExceptions.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "IO/Network Error fetching details for tvId: $tvId" }
                if (_uiState.value !is TvDetailState.Success) {
                    _uiState.value =
                        TvDetailState.Error(UiText.Resource(Res.string.error_network))
                }
            } catch (e: ContentConvertException) {
                logMalformedResponse(e)
                if (_uiState.value !is TvDetailState.Success) {
                    _uiState.value = TvDetailState.Error(UiText.Resource(Res.string.error_unexpected_tv_details))
                }
            } catch (e: SerializationException) {
                logMalformedResponse(e)
                if (_uiState.value !is TvDetailState.Success) {
                    _uiState.value = TvDetailState.Error(UiText.Resource(Res.string.error_unexpected_tv_details))
                }
            }
        }
    }

    private fun buildSuccessState(
        detail: TvDetail,
        seasonDetails: Map<Int, TvSeasonDetail>,
    ): TvDetailState.Success {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val (currentSeasonNumber, latestReleasedEpisodeNumber) = resolveCurrentSeasonAndEpisode(seasonDetails, today)
        return TvDetailState.Success(
            tvDetail = detail,
            currentSeason = seasonDetails[currentSeasonNumber],
            latestReleasedEpisodeNumber = latestReleasedEpisodeNumber,
            allSeasonDetails = seasonDetails,
            regionCode = regionRepository.getSelectedRegion(),
            fallbackRegionCode = regionRepository.getFallbackRegion(),
        )
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
