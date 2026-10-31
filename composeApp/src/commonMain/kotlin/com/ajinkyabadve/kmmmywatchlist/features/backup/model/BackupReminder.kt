package com.ajinkyabadve.kmmmywatchlist.features.backup.model

import kotlinx.serialization.Serializable

/** [seasonNumber]/[episodeNumber] are both null for a movie or show reminder, both set for an
 *  episode reminder. [releaseDate] is `yyyy-MM-dd`. */
@Serializable
data class BackupReminder(
    val mediaId: Long,
    val mediaType: String,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val title: String,
    val posterPath: String? = null,
    val releaseDate: String,
    val releaseSource: String? = null,
)
