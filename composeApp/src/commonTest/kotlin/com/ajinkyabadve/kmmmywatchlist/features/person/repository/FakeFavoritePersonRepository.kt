package com.ajinkyabadve.kmmmywatchlist.features.person.repository

import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for [FavoritePersonRepository] - deliberately dumb, same reasoning as every
 *  other Fake*Repository in this codebase (see e.g. FakeTrackedMediaRepository's kdoc). */
class FakeFavoritePersonRepository : FavoritePersonRepository {
    private data class Row(
        val id: Long,
        val name: String,
        val profilePath: String?,
        val lastKnownCreditIds: String?,
    )

    private val favoritesFlow = MutableStateFlow<Map<Long, Row>>(emptyMap())
    val setFavoriteCalls = mutableListOf<Pair<Long, Boolean>>()
    val updateLastKnownCreditIdsCalls = mutableListOf<Pair<Long, String>>()
    private val addedAtById = mutableMapOf<Long, Long>()

    fun seedFavorite(
        personId: Long,
        name: String = "Fake Person $personId",
        profilePath: String? = null,
        lastKnownCreditIds: String? = null,
    ) {
        favoritesFlow.value = favoritesFlow.value + (personId to Row(personId, name, profilePath, lastKnownCreditIds))
    }

    override fun observeIsFavorite(personId: Long): Flow<Boolean> = favoritesFlow.map { it.containsKey(personId) }

    override fun observeFavoritePeople(): Flow<List<Person>> =
        favoritesFlow.map { rows -> rows.values.map { Person(id = it.id.toInt(), name = it.name, profilePath = it.profilePath) } }

    override suspend fun setFavorite(
        personId: Long,
        name: String,
        profilePath: String?,
        favorite: Boolean,
    ) {
        setFavoriteCalls.add(personId to favorite)
        favoritesFlow.value =
            if (favorite) {
                favoritesFlow.value + (personId to Row(personId, name, profilePath, favoritesFlow.value[personId]?.lastKnownCreditIds))
            } else {
                favoritesFlow.value - personId
            }
    }

    override suspend fun favoritePeopleForPolling(): List<FavoritePersonPollCandidate> =
        favoritesFlow.value.values.map { FavoritePersonPollCandidate(id = it.id, lastKnownCreditIds = it.lastKnownCreditIds) }

    override suspend fun updateLastKnownCreditIds(
        personId: Long,
        creditIds: String,
    ) {
        updateLastKnownCreditIdsCalls.add(personId to creditIds)
        favoritesFlow.value[personId]?.let {
            favoritesFlow.value = favoritesFlow.value + (personId to it.copy(lastKnownCreditIds = creditIds))
        }
    }

    override suspend fun allForBackup(): List<FavoritePersonRecord> =
        favoritesFlow.value.values.map { FavoritePersonRecord(it.id, it.name, it.profilePath, addedAtById[it.id] ?: 0L) }

    override suspend fun restore(records: List<FavoritePersonRecord>): Int {
        val added = records.filter { it.id !in favoritesFlow.value }
        added.forEach { record ->
            addedAtById[record.id] = record.addedAt
            seedFavorite(personId = record.id, name = record.name, profilePath = record.profilePath)
        }
        return added.size
    }
}
