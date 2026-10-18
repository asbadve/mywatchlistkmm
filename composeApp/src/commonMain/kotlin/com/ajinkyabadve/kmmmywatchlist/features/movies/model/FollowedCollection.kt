package com.ajinkyabadve.kmmmywatchlist.features.movies.model

/** A locally-followed collection, for the "Collections" tab under My Fav. */
data class FollowedCollection(
    val id: Long,
    val name: String,
    val posterPath: String?,
)
