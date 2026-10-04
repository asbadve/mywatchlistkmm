package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieDetailCacheRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieDetailCacheRepositoryImpl
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
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MovieDetailState {
    data object Loading : MovieDetailState

    data class Success(
        val movieDetail: MovieDetail,
        val regionCode: String,
        val fallbackRegionCode: String,
    ) : MovieDetailState

    data class Error(
        val message: UiText,
    ) : MovieDetailState
}

class MovieDetailScreenModel(
    private val movieId: Long,
    private val movieDetailCacheRepository: MovieDetailCacheRepository = MovieDetailCacheRepositoryImpl(),
    private val regionRepository: RegionRepository = RegionRepositoryImpl(),
    authRepository: AuthRepository = AuthRepositoryImpl(),
    accountMediaRepository: AccountMediaRepository = AccountMediaRepositoryImpl(),
    trackedMediaRepository: TrackedMediaRepository = TrackedMediaRepositoryImpl(),
    releaseReminderRepository: ReleaseReminderRepository = ReleaseReminderRepositoryImpl(),
    private val releaseReminderCoordinator: ReleaseReminderCoordinator = ReleaseReminderCoordinator(releaseReminderRepository),
    private val notificationJobSync: NotificationJobSync = NotificationJobSync(releaseReminderRepository = releaseReminderRepository),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<MovieDetailState>(MovieDetailState.Loading)
    val uiState: StateFlow<MovieDetailState> = _uiState.asStateFlow()

    /**
     * Owned here, not by `MediaActionButtons` itself - see [MediaActionsState]'s kdoc for why a
     * reusable composable never gets its own `ViewModel`. Launches on this screen's
     * `viewModelScope`, so the toggle survives past whatever recomposes the hero.
     */
    val mediaActionsState =
        MediaActionsState(MediaTypeConstant.MOVIE, movieId, viewModelScope, accountMediaRepository, trackedMediaRepository)

    private val reminderKey = ReminderKey(movieId, MediaTypeConstant.MOVIE)

    /** Whether a release reminder (checklist item 16) is set for this movie - a local read. */
    val hasReleaseReminder: Flow<Boolean> = releaseReminderRepository.observeHasReminder(reminderKey)

    /** The global reminder time, shown in the hero's caption once a reminder is set. */
    val reminderPreference: Flow<ReminderPreference> = releaseReminderRepository.observePreference()

    init {
        loadMovieDetails()
        // This ViewModel triggers the `account_states` pre-check, not `MediaActionButtons` - the
        // moment a session appears (already logged in, or logging in while this screen is open),
        // not on some composable's recomposition/LaunchedEffect timing.
        viewModelScope.launch { mediaActionsState.loadOnSessionAvailable(authRepository) }
    }

    fun loadMovieDetails() {
        // MovieDetailCacheRepository is the single source of truth: it decides cache-vs-network and
        // writes fresh data through to the DB via NetworkBoundResource - this only renders whatever
        // it emits. See MovieDetailCacheRepository's kdoc and NetworkBoundResource's.
        viewModelScope.launch(Dispatchers.Main) {
            movieDetailCacheRepository.getMovieDetail(movieId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> Unit // _uiState already starts Loading
                    is Resource.Success -> _uiState.value = successState(resource.data)
                    is Resource.Error ->
                        if (resource.data != null) {
                            _uiState.value = successState(resource.data)
                        } else {
                            Napier.e(tag = TAG, throwable = resource.cause) { "Error fetching details for movieId: $movieId" }
                            _uiState.value = MovieDetailState.Error(resource.message)
                        }
                }
            }
        }
    }

    /**
     * Adds or removes this movie's release reminder. The date is resolved for the viewer's region
     * (see [resolveReleaseDate]); a movie with no usable date can't be reminded about, so this is a
     * no-op then. Also keeps the shared background job in step, since a first reminder may need it.
     */
    fun setReleaseReminder(
        detail: MovieDetail,
        regionCode: String,
        fallbackRegionCode: String,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            val resolved = detail.resolveReleaseDate(regionCode, fallbackRegionCode) ?: return@launch
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

    private fun successState(detail: MovieDetail) =
        MovieDetailState.Success(detail, regionRepository.getSelectedRegion(), regionRepository.getFallbackRegion())

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "MovieDetailScreenModel"
    }
}
