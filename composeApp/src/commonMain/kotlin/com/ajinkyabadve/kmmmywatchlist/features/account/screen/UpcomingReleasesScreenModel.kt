package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedTvShowSummary
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.UpcomingDateKind
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.UpcomingMediaItem
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationJobSync
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderCoordinator
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseSource
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseSourceKind
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderKey
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderPreference
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvDetailCacheRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvDetailCacheRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private object UpcomingReleasesScreenModelConstant {
    /** Season fetches run at most this many at a time, so a big watchlist doesn't fire dozens of
     *  TMDB requests at once. */
    const val MAX_PARALLEL_SEASON_REFRESHES = 4
}

/**
 * MyFavTabs' "Upcoming" tab (future_features_checklist.md item 18) - lists every tracked movie not
 * yet released ([TrackedMediaRepository.observeUpcoming]) and every unreleased episode of every
 * tracked TV show. Episode-level items can't come from [TrackedMediaRepository] alone - it only
 * ever knows a show's single *next* episode date, not every unaired episode of the current season
 * (confirmed a real gap 2026-09-28 against a favorited series with several episodes still to air) -
 * so this ScreenModel combines [TrackedMediaRepository.observeTrackedTvShows] with cached
 * per-episode season data from [TvDetailCacheRepository.observeSeasons] instead. Both sources are
 * purely local/reactive (no network call), so there's no loading/error state to render - see
 * `PersonFavoritesScreenModel`'s identical reasoning. A show whose season was never opened in this
 * app has no cached season data yet and simply doesn't contribute episodes until it is - an
 * accepted v1 gap, not a bug.
 */
class UpcomingReleasesScreenModel(
    private val trackedMediaRepository: TrackedMediaRepository = TrackedMediaRepositoryImpl(),
    private val tvDetailCacheRepository: TvDetailCacheRepository = TvDetailCacheRepositoryImpl(),
    releaseReminderRepository: ReleaseReminderRepository = ReleaseReminderRepositoryImpl(),
    private val releaseReminderCoordinator: ReleaseReminderCoordinator = ReleaseReminderCoordinator(releaseReminderRepository),
    private val notificationJobSync: NotificationJobSync = NotificationJobSync(releaseReminderRepository = releaseReminderRepository),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private var refreshJob: Job? = null

    private val _isRefreshing = MutableStateFlow(false)

    /** True while [refresh] is syncing from TMDB - the list itself updates live as rows land. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Pulls fresh data from TMDB so the tab reflects the account without the user first opening
     * the Favorites/Watchlist grids or each show's page: re-syncs favorites/watchlist, then the
     * newest seasons of every tracked show. Everything this tab renders is a live database read,
     * so it simply re-renders as the sync writes rows. A refresh already in flight is not started
     * again.
     */
    fun refresh(
        accountId: Long,
        sessionId: String,
    ) {
        if (refreshJob?.isActive == true) return
        refreshJob =
            viewModelScope.launch {
                _isRefreshing.value = true
                try {
                    trackedMediaRepository.refreshAll(accountId, sessionId)
                    val shows = trackedMediaRepository.observeTrackedTvShows().first()
                    val limit = Semaphore(UpcomingReleasesScreenModelConstant.MAX_PARALLEL_SEASON_REFRESHES)
                    coroutineScope {
                        shows
                            .map { show -> async { limit.withPermit { tvDetailCacheRepository.refreshLatestSeasons(show.id.toLong()) } } }
                            .awaitAll()
                    }
                } finally {
                    _isRefreshing.value = false
                }
            }
    }

    /** Which rows have a release reminder set (checklist item 16), for the bells' set state. */
    val remindedKeys: Flow<Set<ReminderKey>> = releaseReminderRepository.observeReminderKeys()

    /** The global reminder time - not shown on this tab today, kept alongside [remindedKeys] so
     *  the tab and the detail heroes read reminder state from the same place. */
    val reminderPreference: Flow<ReminderPreference> = releaseReminderRepository.observePreference()

    /** Adds or removes the reminder for one row - a movie, or one specific episode of a show. */
    fun setReminder(
        item: UpcomingMediaItem,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            releaseReminderCoordinator.setReminder(item.toReleaseReminder(), enabled)
            notificationJobSync.refresh()
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val upcomingItems: Flow<List<UpcomingMediaItem>> =
        combine(
            trackedMediaRepository.observeUpcoming(),
            trackedMediaRepository.observeTrackedTvShows().flatMapLatest { shows ->
                if (shows.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        shows.map { show ->
                            tvDetailCacheRepository.observeSeasons(show.id.toLong()).map { seasons -> show to seasons }
                        },
                    ) { it.toList() }
                }
            },
        ) { movies, showsWithSeasons ->
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            (movies + buildUpcomingEpisodeItems(showsWithSeasons, today)).sortedBy { it.date }
        }
}

/**
 * Every unreleased episode across [showsWithSeasons]' cached seasons, one [UpcomingMediaItem] per
 * episode - extracted from [UpcomingReleasesScreenModel] so this is unit-testable without a Flow/
 * Compose harness.
 */
internal fun buildUpcomingEpisodeItems(
    showsWithSeasons: List<Pair<TrackedTvShowSummary, Map<Int, TvSeasonDetail>>>,
    today: LocalDate,
): List<UpcomingMediaItem> =
    showsWithSeasons.flatMap { (show, seasons) ->
        seasons.values.flatMap { season ->
            season.episodes.mapNotNull { episode ->
                val airDate = episode.airDate?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
                if (airDate != null && airDate > today) {
                    UpcomingMediaItem(
                        id = show.id,
                        mediaType = MediaTypeConstant.TV,
                        title = show.title,
                        posterPath = show.posterPath,
                        date = airDate,
                        dateKind = UpcomingDateKind.NEXT_EPISODE,
                        seasonNumber = season.seasonNumber,
                        episodeNumber = episode.episodeNumber,
                    )
                } else {
                    null
                }
            }
        }
    }

/** The reminder key for an Upcoming row: an episode row keys on its season/episode, so each
 *  unaired episode can be reminded about on its own. */
fun UpcomingMediaItem.reminderKey(): ReminderKey =
    if (mediaType == MediaTypeConstant.TV && seasonNumber != null && episodeNumber != null) {
        ReminderKey(id.toLong(), MediaTypeConstant.TV, seasonNumber, episodeNumber)
    } else {
        ReminderKey(id.toLong(), mediaType)
    }

internal fun UpcomingMediaItem.toReleaseReminder(): ReleaseReminder {
    val key = reminderKey()
    // A movie row's date is TMDB's primary release date (that's what trackedMedia stores); the
    // poller refines it to the viewer's regional theatrical/digital date on its next run.
    val source = if (key.isEpisode) ReleaseSourceKind.EPISODE_AIR else ReleaseSourceKind.PRIMARY
    return ReleaseReminder(
        key = key,
        title = title,
        posterPath = posterPath,
        releaseDate = date,
        releaseSource = ReleaseSource(source).encode(),
    )
}
