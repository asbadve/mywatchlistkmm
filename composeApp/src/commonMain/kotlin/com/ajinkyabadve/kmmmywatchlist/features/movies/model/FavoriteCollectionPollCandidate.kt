package com.ajinkyabadve.kmmmywatchlist.features.movies.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepository

/** One favorited collection's poll-relevant state, as read by
 *  [FavoriteCollectionRepository.favoriteCollectionsForPolling]. */
data class FavoriteCollectionPollCandidate(
    val id: Long,
    val lastKnownPartIds: String?,
)
