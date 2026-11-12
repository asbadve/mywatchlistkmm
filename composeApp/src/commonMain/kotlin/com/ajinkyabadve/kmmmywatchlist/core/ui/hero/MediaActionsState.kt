package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.sync.QueuedToggleSyncScheduler
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TrackedMediaSnapshot
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
 *
 * Turning a toggle **on** also writes the title into the local tracked-media table
 * ([TrackedMediaRepository.trackLocally]), so the Upcoming timeline - which reads only that table -
 * shows it immediately rather than after the next full refresh.
 *
 * @param trackedMediaSnapshot the title's details as the owning screen currently has them; null
 *   until its detail has loaded, in which case only an existing row is revived.
 */
class MediaActionsState(
    private val mediaType: String,
    private val mediaId: Long,
    private val coroutineScope: CoroutineScope,
    private val accountMediaRepository: AccountMediaRepository = AccountMediaRepositoryImpl(),
    // Test-only seam, same pattern as accountMediaRepository above - lets a test inject a fake so
    // this state holder never hits the real database.
    private val trackedMediaRepository: TrackedMediaRepository = TrackedMediaRepositoryImpl(),
    private val trackedMediaSnapshot: () -> TrackedMediaSnapshot? = { null },
    // Test seam, same shape as MediaActionButtons' scheduleNotificationJob: the real scheduler
    // starts a WorkManager job / background send, which a unit test must never reach.
    private val scheduleQueuedToggleSync: () -> Unit = { QueuedToggleSyncScheduler.schedule() },
) {
    private val _uiState = MutableStateFlow(MediaActionsUiState())
    val uiState: StateFlow<MediaActionsUiState> = _uiState.asStateFlow()

    // One-shot event, not part of MediaActionsUiState: turns true the instant a TV show is
    // favorited/watchlisted ON, so MediaActionButtonsSection can offer the episode-alerts opt-in
    // prompt right there - see EpisodeAlertOptInDialog's kdoc for the full flow. consumeEpisodeAlertPrompt
    // resets it so the same toggle doesn't re-trigger it on recomposition.
    private val _shouldPromptForEpisodeAlerts = MutableStateFlow(false)
    val shouldPromptForEpisodeAlerts: StateFlow<Boolean> = _shouldPromptForEpisodeAlerts.asStateFlow()

    fun consumeEpisodeAlertPrompt() {
        _shouldPromptForEpisodeAlerts.update { false }
    }

    fun load(sessionId: String) {
        coroutineScope.launch {
            // Offline-first: show what this device knows (the local favorites/watchlist, offline
            // changes included) straight away. TMDB's account_states below replaces it when it
            // answers; offline it never does, and the icons used to fall back to "off" even for a
            // title in your favorites.
            val local = trackedMediaRepository.trackedCategories(mediaId.toInt(), mediaType)
            _uiState.update {
                it.copy(
                    isFavorite = AccountMediaCategory.FAVORITES in local,
                    isInWatchlist = AccountMediaCategory.WATCHLIST in local,
                    isLoading = false,
                )
            }
            runCatchingApiCall("loading account states for $mediaType/$mediaId") {
                val states = accountMediaRepository.getAccountStates(sessionId, mediaType, mediaId)
                _uiState.update { it.copy(isFavorite = states.favorite, isInWatchlist = states.watchlist, isLoading = false) }
            }
            // Also clears the shimmer on failure, in case the local read above didn't run first.
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleFavorite(
        accountId: Long,
        sessionId: String,
    ) {
        val newValue = !_uiState.value.isFavorite
        _uiState.update { it.copy(isFavorite = newValue) }
        if (newValue && mediaType == MediaTypeConstant.TV) _shouldPromptForEpisodeAlerts.update { true }
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
            settleToggle(outcome, AccountMediaCategory.FAVORITES, accountId, newValue) {
                _uiState.update { it.copy(isFavorite = !newValue) }
            }
        }
    }

    fun toggleWatchlist(
        accountId: Long,
        sessionId: String,
    ) {
        val newValue = !_uiState.value.isInWatchlist
        _uiState.update { it.copy(isInWatchlist = newValue) }
        if (newValue && mediaType == MediaTypeConstant.TV) _shouldPromptForEpisodeAlerts.update { true }
        coroutineScope.launch {
            if (!newValue) {
                trackedMediaRepository.markPendingDelete(mediaId.toInt(), mediaType, AccountMediaCategory.WATCHLIST)
            }
            val outcome =
                runCatchingApiCall("toggling watchlist for $mediaType/$mediaId") {
                    accountMediaRepository.setWatchlist(accountId, sessionId, mediaType, mediaId, newValue)
                }
            settleToggle(outcome, AccountMediaCategory.WATCHLIST, accountId, newValue) {
                _uiState.update { it.copy(isInWatchlist = !newValue) }
            }
        }
    }

    /**
     * The local side of a favorite/watchlist toggle once its TMDB call resolved - shared by
     * [toggleFavorite] and [toggleWatchlist], which differ only in [category] and which icon
     * [rollBackIcon] resets.
     * - Sent: confirm a removal, or write an addition locally; drop any toggle still queued for
     *   this title, since this one supersedes it.
     * - Offline: the icon keeps its new state. A removal stays hidden locally, an addition is
     *   written locally, and the toggle is queued (see [TrackedMediaRepository.queueOfflineToggle])
     *   and a send scheduled for when the network is back (`QueuedToggleSyncScheduler`).
     * - Failed: TMDB rejected it - the icon and any local hide roll back.
     */
    private suspend fun settleToggle(
        outcome: ToggleOutcome,
        category: AccountMediaCategory,
        accountId: Long,
        newValue: Boolean,
        rollBackIcon: () -> Unit,
    ) {
        val id = mediaId.toInt()
        when (outcome) {
            ToggleOutcome.SUCCESS -> {
                if (newValue) trackLocally(category) else trackedMediaRepository.confirmDelete(id, mediaType, category)
                trackedMediaRepository.clearQueuedToggle(id, mediaType, category, accountId)
            }
            ToggleOutcome.OFFLINE -> {
                if (newValue) trackLocally(category)
                trackedMediaRepository.queueOfflineToggle(id, mediaType, category, accountId, newValue)
                scheduleQueuedToggleSync()
            }
            ToggleOutcome.FAILED -> {
                rollBackIcon()
                if (!newValue) trackedMediaRepository.clearPendingDelete(id, mediaType, category)
            }
        }
    }

    /**
     * Writes the title into [category] locally - or, before the owning screen has loaded its
     * detail (no snapshot yet), at least revives a row that was queued for removal.
     */
    private suspend fun trackLocally(category: AccountMediaCategory) {
        val snapshot = trackedMediaSnapshot()
        if (snapshot != null) {
            trackedMediaRepository.trackLocally(snapshot, category)
        } else {
            trackedMediaRepository.clearPendingDelete(mediaId.toInt(), mediaType, category)
        }
    }

    /** [OFFLINE] is distinguished from [FAILED] so a toggle made without connectivity is queued
     *  for the next refresh (see [settleToggle]) instead of bouncing back - only a real
     *  server-side rejection rolls the toggle back. */
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
