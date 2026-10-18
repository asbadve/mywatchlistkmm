package com.ajinkyabadve.kmmmywatchlist.features.person.model

import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepository

/** One favorited person's poll-relevant state, as read by
 *  [FavoritePersonRepository.favoritePeopleForPolling]. */
data class FavoritePersonPollCandidate(
    val id: Long,
    val lastKnownCreditIds: String?,
)
