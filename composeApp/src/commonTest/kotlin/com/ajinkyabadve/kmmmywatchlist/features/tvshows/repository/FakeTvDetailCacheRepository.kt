package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.core.data.Resource
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * In-memory stand-in for [TvDetailCacheRepository] - see
 * [com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeMovieDetailCacheRepository]'s
 * identical kdoc for why this is deliberately dumb (the real orchestration is covered against a
 * real in-memory SQLite database in `desktopTest`'s `TvDetailCacheRepositoryImplTest`).
 */
class FakeTvDetailCacheRepository : TvDetailCacheRepository {
    private val detailFlows = mutableMapOf<Long, MutableStateFlow<TvDetail?>>()
    private val seasonsFlows = mutableMapOf<Long, MutableStateFlow<Map<Int, TvSeasonDetail>>>()
    var getTvDetailResult: Result<TvDetail> = Result.success(TvDetail())

    /** Seasons returned alongside a successful [getTvDetailResult] - defaults to empty so tests
     *  that don't care about seasons don't have to set it. */
    var getTvDetailSeasonsResult: Map<Int, TvSeasonDetail> = emptyMap()
    val getTvDetailCalls = mutableListOf<Long>()

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

    override fun getTvDetail(tvId: Long): Flow<Resource<Pair<TvDetail, Map<Int, TvSeasonDetail>>>> =
        flow {
            getTvDetailCalls.add(tvId)
            val cachedDetail = detailFlowFor(tvId).value
            val cached = cachedDetail?.let { it to seasonsFlowFor(tvId).value }
            getTvDetailResult.fold(
                onSuccess = { detail ->
                    detailFlowFor(tvId).value = detail
                    seasonsFlowFor(tvId).value = getTvDetailSeasonsResult
                    emit(Resource.Success(detail to getTvDetailSeasonsResult))
                },
                onFailure = { throwable ->
                    emit(Resource.Error(throwable, UiText.Plain(throwable.message ?: "error"), cached))
                },
            )
        }

    private fun detailFlowFor(tvId: Long): MutableStateFlow<TvDetail?> = detailFlows.getOrPut(tvId) { MutableStateFlow(null) }

    private fun seasonsFlowFor(tvId: Long): MutableStateFlow<Map<Int, TvSeasonDetail>> =
        seasonsFlows.getOrPut(tvId) { MutableStateFlow(emptyMap()) }
}
