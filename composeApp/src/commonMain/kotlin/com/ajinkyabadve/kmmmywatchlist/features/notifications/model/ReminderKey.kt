package com.ajinkyabadve.kmmmywatchlist.features.notifications.model

/**
 * Identifies one reminder: a movie, a show (its premiere), or one episode of a show
 * ([seasonNumber] and [episodeNumber] both set).
 */
data class ReminderKey(
    val mediaId: Long,
    val mediaType: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
) {
    val isEpisode: Boolean get() = seasonNumber != null && episodeNumber != null
}
