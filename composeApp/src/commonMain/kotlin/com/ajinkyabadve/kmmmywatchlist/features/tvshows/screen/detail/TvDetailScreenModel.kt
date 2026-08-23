package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.data.Resource
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
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

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
        // show and per season) and writes fresh data through to the DB via NetworkBoundResource -
        // this only renders whatever it emits. See TvDetailCacheRepository's kdoc and
        // NetworkBoundResource's.
        viewModelScope.launch(Dispatchers.Main) {
            tvDetailCacheRepository.getTvDetail(tvId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> Unit // _uiState already starts Loading
                    is Resource.Success -> _uiState.value = buildSuccessState(resource.data.first, resource.data.second)
                    is Resource.Error ->
                        if (resource.data != null) {
                            _uiState.value = buildSuccessState(resource.data.first, resource.data.second)
                        } else {
                            Napier.e(tag = TAG, throwable = resource.cause) { "Error fetching details for tvId: $tvId" }
                            _uiState.value = TvDetailState.Error(resource.message)
                        }
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

    private companion object {
        const val TAG = "TvDetailScreenModel"
    }
}
