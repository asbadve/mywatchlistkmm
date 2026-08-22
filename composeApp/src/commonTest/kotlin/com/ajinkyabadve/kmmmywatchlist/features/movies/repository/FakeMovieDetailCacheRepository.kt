package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory stand-in for [MovieDetailCacheRepository] - screen model tests need this so they never
 * touch [com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider]'s real platform driver. Repository
 * behavior itself (JSON round-trip through real SQLite) is covered in `desktopTest`'s
 * `MovieDetailCacheRepositoryImplTest` instead - this fake is deliberately dumb: [refresh] just
 * returns/throws [refreshResult] and, on success, pushes the result into the same
 * [MutableStateFlow] [observe] reads from - mirroring the real impl's write-through-on-success
 * behavior without touching a database.
 */
class FakeMovieDetailCacheRepository : MovieDetailCacheRepository {
    private val flows = mutableMapOf<Long, MutableStateFlow<MovieDetail?>>()
    var refreshResult: Result<MovieDetail> = Result.success(MovieDetail())
    val refreshCalls = mutableListOf<Long>()

    fun seedCached(
        movieId: Long,
        detail: MovieDetail,
    ) {
        flowFor(movieId).value = detail
    }

    override fun observe(movieId: Long): Flow<MovieDetail?> = flowFor(movieId)

    override suspend fun refresh(movieId: Long): MovieDetail {
        refreshCalls.add(movieId)
        val detail = refreshResult.getOrThrow()
        flowFor(movieId).value = detail
        return detail
    }

    private fun flowFor(movieId: Long): MutableStateFlow<MovieDetail?> = flows.getOrPut(movieId) { MutableStateFlow(null) }
}
