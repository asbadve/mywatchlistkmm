package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import com.ajinkyabadve.kmmmywatchlist.features.account.repository.AccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.AccountMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

data class MediaActionsUiState(
    val isFavorite: Boolean = false,
    val isInWatchlist: Boolean = false,
    // Starts true: the icons render a shimmer placeholder (see MediaActionButtons) rather than a
    // guessed favorite/watchlist state until account_states actually answers - this never blocks
    // the rest of the detail screen, which loads on its own separate ScreenModel call.
    val isLoading: Boolean = true,
)

/**
 * Owns every network call behind a title's favorite/watchlist icons - pre-checking their state via
 * `account_states` on load, then toggling each on click.
 *
 * Deliberately a **plain class, not a `ViewModel`**: `MediaActionButtons` is a reusable composable
 * (shared by `MovieHeroSection` and `TvHeroSection`, not a screen/destination itself), and a
 * `ViewModel` per reusable widget instance is the anti-pattern Android's own architecture guidance
 * warns against - "you shouldn't pass `ViewModel` instances down to other composables"; ViewModels
 * are scoped to a `ViewModelStoreOwner` (an Activity/Fragment/nav destination), not to a widget that
 * happens to render in several places (https://developer.android.com/develop/ui/compose/state-hoisting).
 * Instead this is a state holder **owned by the screen's own ViewModel**
 * (`MovieDetailScreenModel.mediaActionsState` / `TvDetailScreenModel.mediaActionsState`), launching
 * work on the [coroutineScope] that ViewModel passes in - which is that ScreenModel's
 * `viewModelScope`, so the work is cancelled with the screen, not with this composable's
 * recomposition lifecycle. See code-conventions §7.
 *
 * [load] is called by the owning ScreenModel, not by `MediaActionButtons` itself: the ScreenModel
 * subscribes to `AuthRepository.sessionState` in its own `init` block and calls `load()` the moment
 * a session appears. The composable never decides *when* to fetch - it only renders [uiState] and
 * forwards click events to [toggleFavorite]/[toggleWatchlist], per code-conventions §6 ("a
 * composable never triggers an API call, not even indirectly via a state holder's method from a
 * `LaunchedEffect`" - the ViewModel triggers it).
 */
class MediaActionsState(
    private val mediaType: String,
    private val mediaId: Long,
    private val coroutineScope: CoroutineScope,
    private val accountMediaRepository: AccountMediaRepository = AccountMediaRepositoryImpl(),
    // Test-only seam, same pattern as accountMediaRepository above - lets a test inject a fake so
    // this state holder never hits the real database.
    private val trackedMediaRepository: TrackedMediaRepository = TrackedMediaRepositoryImpl(),
) {
    private val _uiState = MutableStateFlow(MediaActionsUiState())
    val uiState: StateFlow<MediaActionsUiState> = _uiState.asStateFlow()

    fun load(sessionId: String) {
        coroutineScope.launch {
            runCatchingApiCall("loading account states for $mediaType/$mediaId") {
                val states = accountMediaRepository.getAccountStates(sessionId, mediaType, mediaId)
                _uiState.update { it.copy(isFavorite = states.favorite, isInWatchlist = states.watchlist, isLoading = false) }
            }
            // Also clears the shimmer on failure - an unknown favorite/watchlist state is shown as
            // unset rather than stuck loading forever; the next successful load corrects it.
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleFavorite(
        accountId: Long,
        sessionId: String,
    ) {
        val newValue = !_uiState.value.isFavorite
        _uiState.update { it.copy(isFavorite = newValue) }
        coroutineScope.launch {
            if (!newValue) {
                // Hide it locally right away, regardless of whether the call below succeeds - see
                // TrackedMediaRepository.markPendingDelete's kdoc.
                trackedMediaRepository.markPendingDelete(mediaId.toInt(), mediaType, AccountMediaCategory.FAVORITES)
            }
            val outcome =
                runCatchingApiCall("toggling favorite for $mediaType/$mediaId") {
                    accountMediaRepository.setFavorite(accountId, sessionId, mediaType, mediaId, newValue)
                }
            when (outcome) {
                ToggleOutcome.SUCCESS ->
                    if (!newValue) {
                        trackedMediaRepository.confirmDelete(mediaId.toInt(), mediaType, AccountMediaCategory.FAVORITES)
                    } else {
                        trackedMediaRepository.clearPendingDelete(mediaId.toInt(), mediaType, AccountMediaCategory.FAVORITES)
                    }
                // Offline - leave it queued, both the local hide and the optimistic icon state stay
                // put; TrackedMediaRepository.sync flushes this the next time Favorites loads online.
                ToggleOutcome.OFFLINE -> Unit
                ToggleOutcome.FAILED -> {
                    _uiState.update { it.copy(isFavorite = !newValue) }
                    if (!newValue) trackedMediaRepository.clearPendingDelete(mediaId.toInt(), mediaType, AccountMediaCategory.FAVORITES)
                }
            }
        }
    }

    fun toggleWatchlist(
        accountId: Long,
        sessionId: String,
    ) {
        val newValue = !_uiState.value.isInWatchlist
        _uiState.update { it.copy(isInWatchlist = newValue) }
        coroutineScope.launch {
            if (!newValue) {
                trackedMediaRepository.markPendingDelete(mediaId.toInt(), mediaType, AccountMediaCategory.WATCHLIST)
            }
            val outcome =
                runCatchingApiCall("toggling watchlist for $mediaType/$mediaId") {
                    accountMediaRepository.setWatchlist(accountId, sessionId, mediaType, mediaId, newValue)
                }
            when (outcome) {
                ToggleOutcome.SUCCESS ->
                    if (!newValue) {
                        trackedMediaRepository.confirmDelete(mediaId.toInt(), mediaType, AccountMediaCategory.WATCHLIST)
                    } else {
                        trackedMediaRepository.clearPendingDelete(mediaId.toInt(), mediaType, AccountMediaCategory.WATCHLIST)
                    }
                ToggleOutcome.OFFLINE -> Unit
                ToggleOutcome.FAILED -> {
                    _uiState.update { it.copy(isInWatchlist = !newValue) }
                    if (!newValue) trackedMediaRepository.clearPendingDelete(mediaId.toInt(), mediaType, AccountMediaCategory.WATCHLIST)
                }
            }
        }
    }

    /** [OFFLINE] is distinguished from [FAILED] so a toggle made without connectivity stays queued
     *  (see [markPendingDelete][TrackedMediaRepository.markPendingDelete]) instead of bouncing back -
     *  only a real server-side rejection rolls the toggle back. */
    private enum class ToggleOutcome { SUCCESS, OFFLINE, FAILED }

    private suspend fun runCatchingApiCall(
        action: String,
        block: suspend () -> Unit,
    ): ToggleOutcome =
        try {
            block()
            ToggleOutcome.SUCCESS
        } catch (e: HttpExceptions) {
            Napier.e(tag = TAG, throwable = e) { "Http error $action" }
            ToggleOutcome.FAILED
        } catch (e: IOException) {
            Napier.e(tag = TAG, throwable = e) { "Network error $action - queued for next sync" }
            ToggleOutcome.OFFLINE
        } catch (e: ContentConvertException) {
            Napier.e(tag = TAG, throwable = e) { "Malformed response $action" }
            ToggleOutcome.FAILED
        } catch (e: SerializationException) {
            Napier.e(tag = TAG, throwable = e) { "Malformed response $action" }
            ToggleOutcome.FAILED
        }

    private companion object {
        const val TAG = "MediaActionsState"
    }
}

/**
 * Watches [authRepository]'s session and calls [MediaActionsState.load] the moment one appears -
 * shared by `MovieDetailScreenModel` and `TvDetailScreenModel`'s `init` blocks (each does
 * `viewModelScope.launch { mediaActionsState.loadOnSessionAvailable(authRepository) }`) so the
 * identical collect loop isn't duplicated between them, per code-conventions §2c. The `launch` call
 * itself stays in the ViewModel - this is only the shared body, so the ViewModel remains the one
 * that decides to start observing (code-conventions §6).
 */
suspend fun MediaActionsState.loadOnSessionAvailable(authRepository: AuthRepository) {
    authRepository.sessionState.collect { session ->
        if (session != null) load(session.sessionId)
    }
}
