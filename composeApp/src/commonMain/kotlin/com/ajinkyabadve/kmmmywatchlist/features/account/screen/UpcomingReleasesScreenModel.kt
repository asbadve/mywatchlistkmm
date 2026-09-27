package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedTvShowSummary
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.UpcomingDateKind
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.UpcomingMediaItem
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvDetailCacheRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvDetailCacheRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

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
    trackedMediaRepository: TrackedMediaRepository = TrackedMediaRepositoryImpl(),
    tvDetailCacheRepository: TvDetailCacheRepository = TvDetailCacheRepositoryImpl(),
) : ViewModel() {
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
