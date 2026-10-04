package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MoviePageResult
import com.ajinkyabadve.kmmmywatchlist.features.trending.repository.TrendingRepository
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
class FakeTrendingRepository : TrendingRepository {
    var getTrendingResult: Result<MoviePageResult> =
        Result.success(
            MoviePageResult(
                page = 1,
                list = emptyList(),
                totalResults = 0,
                totalPages = 0,
            ),
        )

    val getTrendingCalls = mutableListOf<Pair<String, String>>()

    /** When set, every call suspends until it completes - lets a test hold the trending rows'
     *  first load open. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun getTrending(
        timeWindow: String,
        mediaType: String,
    ): MoviePageResult {
        getTrendingCalls.add(timeWindow to mediaType)
        gate?.await()
        val result = getTrendingResult
        if (result.isSuccess) {
            return result.getOrThrow()
        } else {
            throw result.exceptionOrNull() ?: IOException("Fake repository error")
        }
    }
}
