package com.ajinkyabadve.kmmmywatchlist.features.account.viewmodel

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.data.Resource
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TrackedTvShowSummary
import com.ajinkyabadve.kmmmywatchlist.features.account.model.UpcomingDateKind
import com.ajinkyabadve.kmmmywatchlist.features.account.model.UpcomingMediaItem
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieDetailCacheRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieDetailCacheRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationJobSync
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderCoordinator
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseSource
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseSourceKind
import com.ajinkyabadve.kmmmywatchlist.features.notifications.model.ReleaseReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.model.ReminderKey
import com.ajinkyabadve.kmmmywatchlist.features.notifications.model.ReminderPreference
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.resolveReleaseDate
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private object UpcomingReleasesScreenModelConstant {
    /** Season fetches run at most this many at a time, so a big watchlist doesn't fire dozens of
     *  TMDB requests at once. */
    const val MAX_PARALLEL_SEASON_REFRESHES = 4

    /** Whose release date a movie row falls back to when the viewer's own region has none. */
    const val FALLBACK_RELEASE_REGION = "US"
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
    private val movieDetailCacheRepository: MovieDetailCacheRepository = MovieDetailCacheRepositoryImpl(),
    private val regionRepository: RegionRepository = RegionRepositoryImpl(),
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
     * newest seasons of every tracked show and the details (with regional release dates) of every
     * upcoming movie. Everything this tab renders is a live database read,
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
                    val movies = trackedMediaRepository.observeUpcoming().first()
                    coroutineScope {
                        val seasonJobs =
                            shows.map { show ->
                                async { limit.withPermit { tvDetailCacheRepository.refreshLatestSeasons(show.id.toLong()) } }
                            }
                        // A failed fetch comes back as Resource.Error, not a throw - the row then
                        // keeps whatever detail was cached, or its primary date.
                        val movieJobs =
                            movies.map { movie ->
                                async {
                                    limit.withPermit {
                                        movieDetailCacheRepository.getMovieDetail(movie.id.toLong()).first { it !is Resource.Loading }
                                    }
                                }
                            }
                        (seasonJobs + movieJobs).awaitAll()
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
            trackedMediaRepository.observeUpcoming().flatMapLatest { movies ->
                if (movies.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        movies.map { movie -> movieDetailCacheRepository.observe(movie.id.toLong()).map { detail -> movie to detail } },
                    ) { it.toList() }
                }
            },
            trackedMediaRepository.observeTrackedTvShows().flatMapLatest { shows ->
                if (shows.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        shows.map { show ->
                            combine(
                                tvDetailCacheRepository.observeSeasons(show.id.toLong()),
                                tvDetailCacheRepository.observe(show.id.toLong()),
                            ) { seasons, detail -> Triple(show, seasons, detail?.airingRegion()) }
                        },
                    ) { it.toList() }
                }
            },
        ) { moviesWithDetails, shows ->
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val movies = resolveUpcomingMovieItems(moviesWithDetails, regionRepository.getSelectedRegion(), today)
            val episodes =
                buildUpcomingEpisodeItems(
                    showsWithSeasons = shows.map { (show, seasons, _) -> show to seasons },
                    today = today,
                    airingRegions = shows.mapNotNull { (show, _, region) -> region?.let { show.id to it } }.toMap(),
                )
            (movies + episodes).sortedBy { it.date }
        }
}

/**
 * Re-dates each upcoming movie to its release in [regionCode] - falling back to the US release,
 * then to TMDB's primary date (all a movie with no cached detail has) - and drops the ones already
 * out there. Theatrical beats digital and festival premieres never count; see
 * [MovieDetail.resolveReleaseDate]. A primary date is labelled with the film's origin country,
 * where it normally applies, so every row with a cached detail carries a flag. Extracted from [UpcomingReleasesScreenModel] so it's unit-testable
 * without a Flow/Compose harness.
 */
internal fun resolveUpcomingMovieItems(
    moviesWithDetails: List<Pair<UpcomingMediaItem, MovieDetail?>>,
    regionCode: String,
    today: LocalDate,
): List<UpcomingMediaItem> =
    moviesWithDetails.mapNotNull { (movie, detail) ->
        val resolved = detail?.resolveReleaseDate(regionCode, UpcomingReleasesScreenModelConstant.FALLBACK_RELEASE_REGION)
        val source =
            resolved?.source?.let { source ->
                if (source.regionCode != null) source else source.copy(regionCode = detail.originCountry.firstOrNull { it.isNotBlank() })
            }
        val item = if (resolved == null) movie else movie.copy(date = resolved.date, releaseSource = source)
        item.takeIf { it.date >= today }
    }

/**
 * Every unreleased episode across [showsWithSeasons]' cached seasons, one [UpcomingMediaItem] per
 * episode - extracted from [UpcomingReleasesScreenModel] so this is unit-testable without a Flow/
 * Compose harness.
 */
internal fun buildUpcomingEpisodeItems(
    showsWithSeasons: List<Pair<TrackedTvShowSummary, Map<Int, TvSeasonDetail>>>,
    today: LocalDate,
    airingRegions: Map<Int, String> = emptyMap(),
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
                        releaseSource = airingRegions[show.id]?.let { ReleaseSource(ReleaseSourceKind.EPISODE_AIR, it) },
                    )
                } else {
                    null
                }
            }
        }
    }

/**
 * Where this show's episode air dates apply: its (first) network's country (Fox -> US), or - for a
 * streaming service TMDB gives no country, like Apple TV - the show's own origin country.
 */
internal fun TvDetail.airingRegion(): String? =
    networks.orEmpty().firstNotNullOfOrNull { it.originCountry.takeIf(String::isNotBlank) }
        ?: originCountry.firstOrNull { it.isNotBlank() }

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
    // A movie row already carries the regional date it shows (releaseSource); one with no cached
    // detail yet has TMDB's primary date, which the reminder poller refines on its next run.
    val source =
        when {
            key.isEpisode -> ReleaseSource(ReleaseSourceKind.EPISODE_AIR)
            else -> releaseSource ?: ReleaseSource(ReleaseSourceKind.PRIMARY)
        }
    return ReleaseReminder(
        key = key,
        title = title,
        posterPath = posterPath,
        releaseDate = date,
        releaseSource = source.encode(),
    )
}
