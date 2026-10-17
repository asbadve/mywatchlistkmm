package com.ajinkyabadve.kmmmywatchlist.features.account.model

import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseSource
import kotlinx.datetime.LocalDate

/**
 * One row of the "Upcoming" tab (future_features_checklist.md item 18) - a tracked movie not yet
 * released ([TrackedMediaRepository.observeUpcoming], [dateKind] = [UpcomingDateKind.MOVIE_RELEASE],
 * [seasonNumber]/[episodeNumber] null), or one specific unreleased episode of a tracked TV show
 * ([dateKind] = [UpcomingDateKind.NEXT_EPISODE], built at the ScreenModel layer from
 * [TrackedMediaRepository.observeTrackedTvShows] combined with cached season data - see
 * `UpcomingReleasesScreenModel`'s kdoc for why episode-level items don't come from this repository
 * directly). A show with several unreleased episodes produces one [UpcomingMediaItem] per episode,
 * not one per show.
 *
 * @param releaseSource for a movie, which release [date] is - e.g. US theatrical - once the
 *   ScreenModel has resolved it against the movie's cached release dates; its region drives the
 *   flag next to the date. Null for episodes, and for a movie with no cached detail yet ([date] is
 *   then TMDB's primary date).
 */
data class UpcomingMediaItem(
    val id: Int,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val date: LocalDate,
    val dateKind: UpcomingDateKind,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val releaseSource: ReleaseSource? = null,
)
