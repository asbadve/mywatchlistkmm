package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for [FavoriteCollectionRepository] - deliberately dumb, same reasoning as
 *  every other Fake*Repository in this codebase (see e.g. FakeFavoritePersonRepository's kdoc). */
class FakeFavoriteCollectionRepository : FavoriteCollectionRepository {
    private data class Row(
        val id: Long,
        val name: String,
        val posterPath: String?,
        val lastKnownPartIds: String?,
    )

    private val favoritesFlow = MutableStateFlow<Map<Long, Row>>(emptyMap())
    val setFavoriteCalls = mutableListOf<Pair<Long, Boolean>>()
    val updateLastKnownPartIdsCalls = mutableListOf<Pair<Long, String>>()

    fun seedFavorite(
        collectionId: Long,
        name: String = "Fake Collection $collectionId",
        posterPath: String? = null,
        lastKnownPartIds: String? = null,
    ) {
        favoritesFlow.value = favoritesFlow.value + (collectionId to Row(collectionId, name, posterPath, lastKnownPartIds))
    }

    override fun observeIsFavorite(collectionId: Long): Flow<Boolean> = favoritesFlow.map { it.containsKey(collectionId) }

    override fun observeFavoriteCollections(): Flow<List<FollowedCollection>> =
        favoritesFlow.map { rows -> rows.values.map { FollowedCollection(id = it.id, name = it.name, posterPath = it.posterPath) } }

    override suspend fun setFavorite(
        collectionId: Long,
        name: String,
        posterPath: String?,
        favorite: Boolean,
    ) {
        setFavoriteCalls.add(collectionId to favorite)
        favoritesFlow.value =
            if (favorite) {
                favoritesFlow.value +
                    (collectionId to Row(collectionId, name, posterPath, favoritesFlow.value[collectionId]?.lastKnownPartIds))
            } else {
                favoritesFlow.value - collectionId
            }
    }

    override suspend fun favoriteCollectionsForPolling(): List<FavoriteCollectionPollCandidate> =
        favoritesFlow.value.values.map { FavoriteCollectionPollCandidate(id = it.id, lastKnownPartIds = it.lastKnownPartIds) }

    override suspend fun updateLastKnownPartIds(
        collectionId: Long,
        partIds: String,
    ) {
        updateLastKnownPartIdsCalls.add(collectionId to partIds)
        favoritesFlow.value[collectionId]?.let {
            favoritesFlow.value = favoritesFlow.value + (collectionId to it.copy(lastKnownPartIds = partIds))
        }
    }
}
