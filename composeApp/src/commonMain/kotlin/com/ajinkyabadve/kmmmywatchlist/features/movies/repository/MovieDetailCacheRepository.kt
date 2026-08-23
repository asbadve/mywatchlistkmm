package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.core.data.NetworkBoundResource
import com.ajinkyabadve.kmmmywatchlist.core.data.Resource
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
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_unexpected_movie_details

private object MovieDetailCacheRepositoryConstant {
    const val TAG = "MovieDetailCacheRepository"
}

/**
 * Local SQLite is the single source of truth for movie detail - this repository, not its callers,
 * decides when to serve cached data vs. trigger a network refresh (see `MyDatabase.sq`'s
 * `movieDetailCache` table for why the whole payload is stored as one JSON blob rather than
 * flattened columns). [observe] never talks to the network; [getMovieDetail] is backed by
 * [NetworkBoundResource] - it fetches from TMDB and writes through to the cache on success only,
 * so a failed refresh never overwrites good cached data.
 */
interface MovieDetailCacheRepository {
    /** Re-emits whenever [getMovieDetail] (or anything else) updates the cached row for [movieId]. */
    fun observe(movieId: Long): Flow<MovieDetail?>

    /** Cache-first, network-backed - see [NetworkBoundResource]'s kdoc for the Loading/Success/
     *  Error shape and its "keep showing cached data through a failed refresh" behavior. */
    fun getMovieDetail(movieId: Long): Flow<Resource<MovieDetail>>
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

    override fun getMovieDetail(movieId: Long): Flow<Resource<MovieDetail>> =
        object : NetworkBoundResource<MovieDetail, MovieDetail>() {
            override fun shouldFetch(data: MovieDetail?) = true

            override fun loadFromDb(): Flow<MovieDetail?> = observe(movieId)

            override suspend fun fetchFromNetwork(): MovieDetail = movieRepository.getMovieDetails(movieId)

            override suspend fun saveCallResult(item: MovieDetail) {
                databaseProvider().myDatabaseQueries.upsertMovieDetailCache(
                    id = movieId,
                    json = Json.encodeToString(MovieDetail.serializer(), item),
                    lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
                )
            }

            override fun malformedResponseMessage(): UiText = UiText.Resource(Res.string.error_unexpected_movie_details)
        }.asFlow()

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
