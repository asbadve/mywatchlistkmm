package com.ajinkyabadve.kmmmywatchlist.features.notifications.model

import kotlinx.datetime.LocalDate

/** One stored release reminder. [releaseSource] says which date won (e.g. `theatrical:IN`). */
data class ReleaseReminder(
    val key: ReminderKey,
    val title: String,
    val posterPath: String?,
    val releaseDate: LocalDate,
    val releaseSource: String?,
)
