package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private object TvDetailCacheRepositoryConstant {
    const val TAG = "TvDetailCacheRepository"
}

/**
 * Local SQLite is the single source of truth for TV detail/season data - see
 * [com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieDetailCacheRepository]'s kdoc for
 * the shared reasoning (this is the TV counterpart, with an extra per-season table since a show's
 * detail fetch fans out to one season fetch per season). [observe]/[observeSeasons] never talk to the
 * network; [refresh] fetches the show and every season from TMDB and writes through to the cache -
 * including falling back to whatever season is already cached when one season's fetch fails, so a
 * single network hiccup never blanks out a season the app already knows about.
 */
interface TvDetailCacheRepository {
    fun observe(tvId: Long): Flow<TvDetail?>

    fun observeSeasons(tvId: Long): Flow<Map<Int, TvSeasonDetail>>

    suspend fun refresh(tvId: Long): TvDetail
}

class TvDetailCacheRepositoryImpl(
    private val tvRepository: TvRepository = TvRepositoryImpl(),
    private val databaseProvider: suspend () -> MyDatabase = { AppDatabaseProvider.get() },
) : TvDetailCacheRepository {
    override fun observe(tvId: Long): Flow<TvDetail?> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectTvDetailCache(tvId)
                    .asFlow()
                    .mapToOneOrNull(Dispatchers.Default)
                    .map { row -> row?.let { decodeDetail(it.json, tvId) } },
            )
        }

    override fun observeSeasons(tvId: Long): Flow<Map<Int, TvSeasonDetail>> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectTvSeasonDetailCacheForTv(tvId)
                    .asFlow()
                    .mapToList(Dispatchers.Default)
                    .map { rows ->
                        rows.mapNotNull { decodeSeason(it.json, tvId, it.seasonNumber.toInt()) }.associateBy { it.seasonNumber }
                    },
            )
        }

    override suspend fun refresh(tvId: Long): TvDetail {
        val detail = tvRepository.getTvDetails(tvId)
        val queries = databaseProvider().myDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()
        queries.upsertTvDetailCache(id = tvId, json = Json.encodeToString(TvDetail.serializer(), detail), lastSyncedAt = now)
        val seasonNumbers = detail.seasons?.map { it.seasonNumber }?.filter { it >= 0 } ?: emptyList()
        coroutineScope {
            seasonNumbers.map { seasonNumber -> async { refreshSeason(tvId, seasonNumber) } }.awaitAll()
        }
        return detail
    }

    private suspend fun refreshSeason(
        tvId: Long,
        seasonNumber: Int,
    ) {
        try {
            val season = tvRepository.getSeasonDetails(tvId, seasonNumber)
            databaseProvider().myDatabaseQueries.upsertTvSeasonDetailCache(
                tvId = tvId,
                seasonNumber = seasonNumber.toLong(),
                json = Json.encodeToString(TvSeasonDetail.serializer(), season),
                lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
            )
        } catch (e: HttpExceptions) {
            logSeasonFailure(tvId, seasonNumber, e)
        } catch (e: IOException) {
            logSeasonFailure(tvId, seasonNumber, e)
        } catch (e: ContentConvertException) {
            logSeasonFailure(tvId, seasonNumber, e)
        } catch (e: SerializationException) {
            logSeasonFailure(tvId, seasonNumber, e)
        }
        // On any failure above, the cached row (if any) for this season is simply left as-is -
        // observeSeasons()'s Flow keeps surfacing whatever was already cached, no explicit fallback
        // read needed here.
    }

    private fun logSeasonFailure(
        tvId: Long,
        seasonNumber: Int,
        throwable: Throwable,
    ) {
        Napier.e(
            tag = TvDetailCacheRepositoryConstant.TAG,
            throwable = throwable,
        ) { "Failed to refresh season $seasonNumber for tvId: $tvId - keeping any cached value" }
    }

    private fun decodeDetail(
        json: String,
        tvId: Long,
    ): TvDetail? =
        try {
            Json.decodeFromString(TvDetail.serializer(), json)
        } catch (e: SerializationException) {
            Napier.e(tag = TvDetailCacheRepositoryConstant.TAG, throwable = e) { "Corrupt cache row for tvId: $tvId" }
            null
        }

    private fun decodeSeason(
        json: String,
        tvId: Long,
        seasonNumber: Int,
    ): TvSeasonDetail? =
        try {
            Json.decodeFromString(TvSeasonDetail.serializer(), json)
        } catch (e: SerializationException) {
            Napier.e(
                tag = TvDetailCacheRepositoryConstant.TAG,
                throwable = e,
            ) { "Corrupt cache row for tvId: $tvId, season: $seasonNumber" }
            null
        }
}
