package com.ajinkyabadve.kmmmywatchlist.features.person.model

/** One followed person as a backup file carries it (item 15) - [addedAt] is kept so a restore
 *  preserves the "most-recently-followed first" order. */
data class FavoritePersonRecord(
    val id: Long,
    val name: String,
    val profilePath: String?,
    val addedAt: Long,
)
