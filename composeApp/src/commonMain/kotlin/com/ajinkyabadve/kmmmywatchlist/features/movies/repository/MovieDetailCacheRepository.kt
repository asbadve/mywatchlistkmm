package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private object MovieDetailCacheRepositoryConstant {
    const val TAG = "MovieDetailCacheRepository"
}

/**
 * Local SQLite is the single source of truth for movie detail - this repository, not its callers,
 * decides when to serve cached data vs. trigger a network refresh (see `MyDatabase.sq`'s
 * `movieDetailCache` table for why the whole payload is stored as one JSON blob rather than
 * flattened columns). [observe] never talks to the network; [refresh] fetches from TMDB and writes
 * through to the cache on success only, so a failed refresh never overwrites good cached data - a
 * caller never has to decide "which source wins", that's enforced here structurally.
 */
interface MovieDetailCacheRepository {
    /** Re-emits whenever [refresh] (or anything else) updates the cached row for [movieId]. */
    fun observe(movieId: Long): Flow<MovieDetail?>

    /** Fetches from TMDB and writes through to the cache on success. Rethrows on failure so the
     *  caller can decide whether to surface a transient error - the cached row is left untouched. */
    suspend fun refresh(movieId: Long): MovieDetail
}

class MovieDetailCacheRepositoryImpl(
    private val movieRepository: MovieRepository = MovieRepositoryImpl(),
    // Same test seam as TrackedMediaRepositoryImpl's databaseProvider - see its kdoc.
    private val databaseProvider: suspend () -> MyDatabase = { AppDatabaseProvider.get() },
) : MovieDetailCacheRepository {
    override fun observe(movieId: Long): Flow<MovieDetail?> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectMovieDetailCache(movieId)
                    .asFlow()
                    .mapToOneOrNull(Dispatchers.Default)
                    .map { row -> row?.let { decode(it.json, movieId) } },
            )
        }

    override suspend fun refresh(movieId: Long): MovieDetail {
        val detail = movieRepository.getMovieDetails(movieId)
        databaseProvider().myDatabaseQueries.upsertMovieDetailCache(
            id = movieId,
            json = Json.encodeToString(MovieDetail.serializer(), detail),
            lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
        )
        return detail
    }

    private fun decode(
        json: String,
        movieId: Long,
    ): MovieDetail? =
        try {
            Json.decodeFromString(MovieDetail.serializer(), json)
        } catch (e: SerializationException) {
            Napier.e(tag = MovieDetailCacheRepositoryConstant.TAG, throwable = e) { "Corrupt cache row for movieId: $movieId" }
            null
        }
}
