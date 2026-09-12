package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/** A locally-followed collection, for the "Collections" tab under My Fav. */
data class FollowedCollection(
    val id: Long,
    val name: String,
    val posterPath: String?,
)

/** One favorited collection's poll-relevant state, as read by
 *  [FavoriteCollectionRepository.favoriteCollectionsForPolling]. */
data class FavoriteCollectionPollCandidate(
    val id: Long,
    val lastKnownPartIds: String?,
)

/**
 * Local-only "favorite collection" concept - see `MyDatabase.sq`'s `favoriteCollection` table
 * kdoc for why: TMDB has no account-level favorite/follow API for collections either (confirmed
 * against the live OpenAPI docs, same finding as
 * [com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepository]'s kdoc for
 * people), so unlike [com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository]
 * this table isn't a cache of anything TMDB returns - it *is* the source of truth, with nothing to
 * sync against.
 */
interface FavoriteCollectionRepository {
    /** Reactive local read - no `account_states`-style network pre-check exists for collections,
     *  so this is always backed directly by SQLite. */
    fun observeIsFavorite(collectionId: Long): Flow<Boolean>

    /** Every favorited collection, most-recently-followed first, for the "Collections" tab under
     *  My Fav. */
    fun observeFavoriteCollections(): Flow<List<FollowedCollection>>

    suspend fun setFavorite(
        collectionId: Long,
        name: String,
        posterPath: String?,
        favorite: Boolean,
    )

    /** Every favorited collection, for `CollectionNotificationPoller` to iterate. */
    suspend fun favoriteCollectionsForPolling(): List<FavoriteCollectionPollCandidate>

    suspend fun updateLastKnownPartIds(
        collectionId: Long,
        partIds: String,
    )
}

class FavoriteCollectionRepositoryImpl(
    // Same test seam as every other repository in this codebase - see TrackedMediaRepositoryImpl's kdoc.
    private val databaseProvider: suspend () -> MyDatabase = { AppDatabaseProvider.get() },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : FavoriteCollectionRepository {
    override fun observeIsFavorite(collectionId: Long): Flow<Boolean> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectFavoriteCollectionById(collectionId)
                    .asFlow()
                    .mapToOneOrNull(Dispatchers.Default)
                    .map { it != null },
            )
        }

    override fun observeFavoriteCollections(): Flow<List<FollowedCollection>> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectAllFavoriteCollections()
                    .asFlow()
                    .mapToList(Dispatchers.Default)
                    .map { rows -> rows.map { FollowedCollection(id = it.id, name = it.name, posterPath = it.posterPath) } },
            )
        }

    override suspend fun setFavorite(
        collectionId: Long,
        name: String,
        posterPath: String?,
        favorite: Boolean,
    ) {
        val queries = databaseProvider().myDatabaseQueries
        if (favorite) {
            queries.insertFavoriteCollection(collectionId, name, posterPath, now())
        } else {
            queries.deleteFavoriteCollection(collectionId)
        }
    }

    override suspend fun favoriteCollectionsForPolling(): List<FavoriteCollectionPollCandidate> =
        databaseProvider()
            .myDatabaseQueries
            .selectAllFavoriteCollections()
            .awaitAsList()
            .map { FavoriteCollectionPollCandidate(id = it.id, lastKnownPartIds = it.lastKnownPartIds) }

    override suspend fun updateLastKnownPartIds(
        collectionId: Long,
        partIds: String,
    ) {
        databaseProvider().myDatabaseQueries.updateFavoriteCollectionPartIds(partIds, collectionId)
    }
}
