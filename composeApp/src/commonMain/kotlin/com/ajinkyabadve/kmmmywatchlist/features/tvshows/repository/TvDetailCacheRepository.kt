package com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.core.data.NetworkBoundResource
import com.ajinkyabadve.kmmmywatchlist.core.data.Resource
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_unexpected_tv_details

private object TvDetailCacheRepositoryConstant {
    const val TAG = "TvDetailCacheRepository"

    /** The latest season, plus the one before it - a new season is sometimes listed (with dates)
     *  while the previous one is still airing. */
    const val UPCOMING_SEASON_COUNT = 2
}

/**
 * Local SQLite is the single source of truth for TV detail/season data - see
 * [com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieDetailCacheRepository]'s kdoc for
 * the shared reasoning (this is the TV counterpart, with an extra per-season table since a show's
 * detail fetch fans out to one season fetch per season). [observe]/[observeSeasons] never talk to
 * the network; [getTvDetail] is backed by [NetworkBoundResource] - it fetches the show and every
 * season from TMDB and writes through to the cache, falling back to whatever season is already
 * cached when one season's fetch fails, so a single network hiccup never blanks out a season the
 * app already knows about.
 */
interface TvDetailCacheRepository {
    fun observe(tvId: Long): Flow<TvDetail?>

    fun observeSeasons(tvId: Long): Flow<Map<Int, TvSeasonDetail>>

    /** Cache-first, network-backed - see [NetworkBoundResource]'s kdoc for the Loading/Success/
     *  Error shape and its "keep showing cached data through a failed refresh" behavior. */
    fun getTvDetail(tvId: Long): Flow<Resource<Pair<TvDetail, Map<Int, TvSeasonDetail>>>>

    /**
     * Fetches the show plus only its newest seasons and writes them to the cache - where every
     * not-yet-aired episode lives - for screens that list upcoming episodes without the user having
     * opened the show (the "Upcoming" tab). Far cheaper than [getTvDetail]'s every-season fetch for
     * a long-running show. Network failures are logged and leave the cache as it was.
     */
    suspend fun refreshLatestSeasons(tvId: Long)
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

    override fun getTvDetail(tvId: Long): Flow<Resource<Pair<TvDetail, Map<Int, TvSeasonDetail>>>> =
        object : NetworkBoundResource<Pair<TvDetail, Map<Int, TvSeasonDetail>>, Pair<TvDetail, Map<Int, TvSeasonDetail>>>() {
            override fun shouldFetch(data: Pair<TvDetail, Map<Int, TvSeasonDetail>>?) = true

            override fun loadFromDb(): Flow<Pair<TvDetail, Map<Int, TvSeasonDetail>>?> =
                combine(observe(tvId), observeSeasons(tvId)) { detail, seasons -> detail?.let { it to seasons } }

            override suspend fun fetchFromNetwork(): Pair<TvDetail, Map<Int, TvSeasonDetail>> {
                val detail = tvRepository.getTvDetails(tvId)
                val seasonNumbers =
                    detail.seasons
                        ?.map { it.seasonNumber }
                        ?.filter { it >= 0 }
                        .orEmpty()
                val seasons =
                    coroutineScope {
                        seasonNumbers
                            .map { seasonNumber -> async { seasonNumber to fetchSeason(tvId, seasonNumber) } }
                            .awaitAll()
                            .mapNotNull { (seasonNumber, season) -> season?.let { seasonNumber to it } }
                            .toMap()
                    }
                return detail to seasons
            }

            override suspend fun saveCallResult(item: Pair<TvDetail, Map<Int, TvSeasonDetail>>) {
                val (detail, seasons) = item
                saveToCache(tvId, detail, seasons)
            }

            override fun malformedResponseMessage(): UiText = UiText.Resource(Res.string.error_unexpected_tv_details)
        }.asFlow()

    override suspend fun refreshLatestSeasons(tvId: Long) {
        val detail =
            try {
                tvRepository.getTvDetails(tvId)
            } catch (e: HttpExceptions) {
                logDetailFailure(tvId, e)
                return
            } catch (e: IOException) {
                logDetailFailure(tvId, e)
                return
            } catch (e: ContentConvertException) {
                logDetailFailure(tvId, e)
                return
            } catch (e: SerializationException) {
                logDetailFailure(tvId, e)
                return
            }
        // Season 0 is "Specials" - never where a show's next episodes are.
        val latestSeasonNumbers =
            detail.seasons
                ?.map { it.seasonNumber }
                ?.filter { it > 0 }
                ?.sortedDescending()
                ?.take(TvDetailCacheRepositoryConstant.UPCOMING_SEASON_COUNT)
                .orEmpty()
        val seasons = latestSeasonNumbers.mapNotNull { seasonNumber -> fetchSeason(tvId, seasonNumber)?.let { seasonNumber to it } }.toMap()
        saveToCache(tvId, detail, seasons)
    }

    private suspend fun saveToCache(
        tvId: Long,
        detail: TvDetail,
        seasons: Map<Int, TvSeasonDetail>,
    ) {
        val queries = databaseProvider().myDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()
        queries.upsertTvDetailCache(id = tvId, json = Json.encodeToString(TvDetail.serializer(), detail), lastSyncedAt = now)
        seasons.forEach { (seasonNumber, season) ->
            queries.upsertTvSeasonDetailCache(
                tvId = tvId,
                seasonNumber = seasonNumber.toLong(),
                json = Json.encodeToString(TvSeasonDetail.serializer(), season),
                lastSyncedAt = now,
            )
        }
    }

    private fun logDetailFailure(
        tvId: Long,
        throwable: Throwable,
    ) {
        Napier.e(
            tag = TvDetailCacheRepositoryConstant.TAG,
            throwable = throwable,
        ) { "Failed to refresh tvId: $tvId - keeping any cached value" }
    }

    /** A season that fails to fetch is simply absent from [fetchFromNetwork]'s result map, so
     *  [saveCallResult] never overwrites (or deletes) whatever was already cached for it -
     *  [observeSeasons]'s Flow keeps surfacing that cached value untouched. */
    private suspend fun fetchSeason(
        tvId: Long,
        seasonNumber: Int,
    ): TvSeasonDetail? =
        try {
            tvRepository.getSeasonDetails(tvId, seasonNumber)
        } catch (e: HttpExceptions) {
            logSeasonFailure(tvId, seasonNumber, e)
            null
        } catch (e: IOException) {
            logSeasonFailure(tvId, seasonNumber, e)
            null
        } catch (e: ContentConvertException) {
            logSeasonFailure(tvId, seasonNumber, e)
            null
        } catch (e: SerializationException) {
            logSeasonFailure(tvId, seasonNumber, e)
            null
        }

    private fun logSeasonFailure(
        tvId: Long,
        seasonNumber: Int,
        throwable: Throwable,
    ) {
        Napier.e(
            tag = TvDetailCacheRepositoryConstant.TAG,
            throwable = throwable,
        ) { "Failed to fetch season $seasonNumber for tvId: $tvId - keeping any cached value" }
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
