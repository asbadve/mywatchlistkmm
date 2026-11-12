package com.ajinkyabadve.kmmmywatchlist.features.account.model

/**
 * One favorite/watchlist change made offline that hasn't reached TMDB yet - a `trackedMediaOutbox`
 * row (see `MyDatabase.sq`), for one list. [isOn] is the state being sent: true adds the title,
 * false removes it.
 */
data class QueuedToggle(
    val id: Int,
    val mediaType: String,
    val isOn: Boolean,
)
