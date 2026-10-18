package com.ajinkyabadve.kmmmywatchlist.features.movies.model

/** One followed collection as a backup file carries it (item 15) - same reason as
 *  `FavoritePersonRecord` for keeping [addedAt]. */
data class FavoriteCollectionRecord(
    val id: Long,
    val name: String,
    val posterPath: String?,
    val addedAt: Long,
)
