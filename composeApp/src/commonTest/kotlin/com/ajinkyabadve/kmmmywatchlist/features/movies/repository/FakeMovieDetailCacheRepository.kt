package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.core.data.Resource
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * In-memory stand-in for [MovieDetailCacheRepository] - screen model tests need this so they never
 * touch [com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider]'s real platform driver. The
 * [NetworkBoundResource][com.ajinkyabadve.kmmmywatchlist.core.data.NetworkBoundResource]
 * orchestration itself - Loading/cached-data-on-error/exception-to-message mapping - is covered
 * against a real in-memory SQLite database in `desktopTest`'s `MovieDetailCacheRepositoryImplTest`
 * instead; this fake is deliberately dumb, emitting only the terminal [Resource.Success]/
 * [Resource.Error] a ScreenModel actually acts on.
 */
class FakeMovieDetailCacheRepository : MovieDetailCacheRepository {
    private val flows = mutableMapOf<Long, MutableStateFlow<MovieDetail?>>()
    var getMovieDetailResult: Result<MovieDetail> = Result.success(MovieDetail())
    val getMovieDetailCalls = mutableListOf<Long>()

    fun seedCached(
        movieId: Long,
        detail: MovieDetail,
    ) {
        flowFor(movieId).value = detail
    }

    override fun observe(movieId: Long): Flow<MovieDetail?> = flowFor(movieId)

    override fun getMovieDetail(movieId: Long): Flow<Resource<MovieDetail>> =
        flow {
            getMovieDetailCalls.add(movieId)
            val cached = flowFor(movieId).value
            getMovieDetailResult.fold(
                onSuccess = { detail ->
                    flowFor(movieId).value = detail
                    emit(Resource.Success(detail))
                },
                onFailure = { throwable ->
                    emit(Resource.Error(throwable, UiText.Plain(throwable.message ?: "error"), cached))
                },
            )
        }

    private fun flowFor(movieId: Long): MutableStateFlow<MovieDetail?> = flows.getOrPut(movieId) { MutableStateFlow(null) }
}
