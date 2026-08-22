package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory stand-in for [TvDetailCacheRepository] - screen model tests need this so they never
 * touch [com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider]'s real platform driver. Repository
 * behavior itself (JSON round-trip through real SQLite) is covered in `desktopTest`'s
 * `TvDetailCacheRepositoryImplTest` instead - this fake is deliberately dumb: [refresh] returns/
 * throws [refreshResult] and, on success, pushes it (plus [refreshSeasonsResult]) into the same
 * [MutableStateFlow]s [observe]/[observeSeasons] read from, mirroring the real impl's
 * write-through-on-success behavior without a database.
 */
class FakeTvDetailCacheRepository : TvDetailCacheRepository {
    private val detailFlows = mutableMapOf<Long, MutableStateFlow<TvDetail?>>()
    private val seasonsFlows = mutableMapOf<Long, MutableStateFlow<Map<Int, TvSeasonDetail>>>()
    var refreshResult: Result<TvDetail> = Result.success(TvDetail())

    /** Seasons written by a successful [refresh] - defaults to empty so tests that don't care about
     *  seasons don't have to set it. */
    var refreshSeasonsResult: Map<Int, TvSeasonDetail> = emptyMap()
    val refreshCalls = mutableListOf<Long>()

    fun seedCached(
        tvId: Long,
        detail: TvDetail,
    ) {
        detailFlowFor(tvId).value = detail
    }

    fun seedCachedSeason(
        tvId: Long,
        season: TvSeasonDetail,
    ) {
        val flow = seasonsFlowFor(tvId)
        flow.value = flow.value + (season.seasonNumber to season)
    }

    override fun observe(tvId: Long): Flow<TvDetail?> = detailFlowFor(tvId)

    override fun observeSeasons(tvId: Long): Flow<Map<Int, TvSeasonDetail>> = seasonsFlowFor(tvId)

    override suspend fun refresh(tvId: Long): TvDetail {
        refreshCalls.add(tvId)
        val detail = refreshResult.getOrThrow()
        detailFlowFor(tvId).value = detail
        seasonsFlowFor(tvId).value = refreshSeasonsResult
        return detail
    }

    private fun detailFlowFor(tvId: Long): MutableStateFlow<TvDetail?> = detailFlows.getOrPut(tvId) { MutableStateFlow(null) }

    private fun seasonsFlowFor(tvId: Long): MutableStateFlow<Map<Int, TvSeasonDetail>> =
        seasonsFlows.getOrPut(tvId) { MutableStateFlow(emptyMap()) }
}
