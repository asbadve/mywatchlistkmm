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
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationJobSync
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderCoordinator
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderKey
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderPreference
import com.ajinkyabadve.kmmmywatchlist.features.notifications.resolveReleaseDate
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
import kotlinx.coroutines.flow.Flow
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
    releaseReminderRepository: ReleaseReminderRepository = ReleaseReminderRepositoryImpl(),
    private val releaseReminderCoordinator: ReleaseReminderCoordinator = ReleaseReminderCoordinator(releaseReminderRepository),
    private val notificationJobSync: NotificationJobSync = NotificationJobSync(releaseReminderRepository = releaseReminderRepository),
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

    private val reminderKey = ReminderKey(tvId, MediaTypeConstant.TV)

    /** Whether a premiere reminder (checklist item 16) is set for this show - a local read. */
    val hasReleaseReminder: Flow<Boolean> = releaseReminderRepository.observeHasReminder(reminderKey)

    /** The global reminder time, shown in the hero's caption once a reminder is set. */
    val reminderPreference: Flow<ReminderPreference> = releaseReminderRepository.observePreference()

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

    /** Adds or removes this show's premiere reminder (its first air date). No-op for a show with
     *  no usable first air date. Also keeps the shared background job in step. */
    fun setReleaseReminder(
        detail: TvDetail,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            val resolved = detail.resolveReleaseDate() ?: return@launch
            releaseReminderCoordinator.setReminder(
                ReleaseReminder(
                    key = reminderKey,
                    title = detail.title,
                    posterPath = detail.posterPath,
                    releaseDate = resolved.date,
                    releaseSource = resolved.source.encode(),
                ),
                enabled,
            )
            notificationJobSync.refresh()
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
