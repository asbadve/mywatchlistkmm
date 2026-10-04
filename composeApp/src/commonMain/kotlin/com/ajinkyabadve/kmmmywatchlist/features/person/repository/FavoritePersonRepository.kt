package com.ajinkyabadve.kmmmywatchlist.features.person.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/** One followed person as a backup file carries it (item 15) - [addedAt] is kept so a restore
 *  preserves the "most-recently-followed first" order. */
data class FavoritePersonRecord(
    val id: Long,
    val name: String,
    val profilePath: String?,
    val addedAt: Long,
)

/** One favorited person's poll-relevant state, as read by
 *  [FavoritePersonRepository.favoritePeopleForPolling]. */
data class FavoritePersonPollCandidate(
    val id: Long,
    val lastKnownCreditIds: String?,
)

/**
 * Local-only "favorite person" concept - see `MyDatabase.sq`'s `favoritePerson` table kdoc for
 * why: TMDB has no account-level favorite/follow API for people (confirmed 2026-08-26 against the
 * live OpenAPI docs), only movies and TV, so unlike [com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepository]
 * this table isn't a cache of anything TMDB returns - it *is* the source of truth, with nothing to
 * sync against.
 */
interface FavoritePersonRepository {
    /** Reactive local read - no `account_states`-style network pre-check exists for people, so
     *  this is always backed directly by SQLite. */
    fun observeIsFavorite(personId: Long): Flow<Boolean>

    /** Every favorited person, most-recently-followed first, for the Person tab's "Favorites"
     *  sub-tab - reuses the [Person] model the "Popular" sub-tab already renders with
     *  `mediaPersonRow`, so both grids look and behave identically. */
    fun observeFavoritePeople(): Flow<List<Person>>

    suspend fun setFavorite(
        personId: Long,
        name: String,
        profilePath: String?,
        favorite: Boolean,
    )

    /** Every favorited person, for `PersonCreditNotificationPoller` to iterate. */
    suspend fun favoritePeopleForPolling(): List<FavoritePersonPollCandidate>

    suspend fun updateLastKnownCreditIds(
        personId: Long,
        creditIds: String,
    )

    /** Every followed person with its follow time, for a backup export. */
    suspend fun allForBackup(): List<FavoritePersonRecord>

    /** Backup restore: adds the people not already followed and leaves the rest untouched - see
     *  `MyDatabase.sq`'s `restoreFavoritePerson`. Returns how many were added. */
    suspend fun restore(records: List<FavoritePersonRecord>): Int
}

class FavoritePersonRepositoryImpl(
    // Same test seam as every other repository in this codebase - see TrackedMediaRepositoryImpl's kdoc.
    private val databaseProvider: suspend () -> MyDatabase = { AppDatabaseProvider.get() },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : FavoritePersonRepository {
    override fun observeIsFavorite(personId: Long): Flow<Boolean> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectFavoritePersonById(personId)
                    .asFlow()
                    .mapToOneOrNull(Dispatchers.Default)
                    .map { it != null },
            )
        }

    override fun observeFavoritePeople(): Flow<List<Person>> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectAllFavoritePeople()
                    .asFlow()
                    .mapToList(Dispatchers.Default)
                    .map { rows -> rows.map { Person(id = it.id.toInt(), name = it.name, profilePath = it.profilePath) } },
            )
        }

    override suspend fun setFavorite(
        personId: Long,
        name: String,
        profilePath: String?,
        favorite: Boolean,
    ) {
        val queries = databaseProvider().myDatabaseQueries
        if (favorite) {
            queries.insertFavoritePerson(personId, name, profilePath, now())
        } else {
            queries.deleteFavoritePerson(personId)
        }
    }

    override suspend fun favoritePeopleForPolling(): List<FavoritePersonPollCandidate> =
        databaseProvider()
            .myDatabaseQueries
            .selectAllFavoritePeople()
            .awaitAsList()
            .map { FavoritePersonPollCandidate(id = it.id, lastKnownCreditIds = it.lastKnownCreditIds) }

    override suspend fun updateLastKnownCreditIds(
        personId: Long,
        creditIds: String,
    ) {
        databaseProvider().myDatabaseQueries.updateFavoritePersonCreditIds(creditIds, personId)
    }

    override suspend fun allForBackup(): List<FavoritePersonRecord> =
        databaseProvider()
            .myDatabaseQueries
            .selectAllFavoritePeople()
            .awaitAsList()
            .map { FavoritePersonRecord(id = it.id, name = it.name, profilePath = it.profilePath, addedAt = it.addedAt) }

    override suspend fun restore(records: List<FavoritePersonRecord>): Int {
        val queries = databaseProvider().myDatabaseQueries
        val existing =
            queries
                .selectAllFavoritePeople()
                .awaitAsList()
                .map { it.id }
                .toSet()
        val added = records.filter { it.id !in existing }
        queries.transaction {
            added.forEach { queries.restoreFavoritePerson(it.id, it.name, it.profilePath, it.addedAt) }
        }
        return added.size
    }
}
