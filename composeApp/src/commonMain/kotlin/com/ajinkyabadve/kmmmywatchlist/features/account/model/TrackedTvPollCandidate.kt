package com.ajinkyabadve.kmmmywatchlist.features.account.model

/** One TV show's poll-relevant state, as read by [TrackedMediaRepository.trackedTvForPolling]. */
data class TrackedTvPollCandidate(
    val id: Int,
    val lastKnownNextEpisodeAirDate: String?,
    val lastKnownStatus: String?,
)
