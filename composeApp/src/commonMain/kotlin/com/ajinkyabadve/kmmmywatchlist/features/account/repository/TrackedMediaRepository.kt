package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.paging3.QueryPagingSource
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.db.AppDatabaseProvider
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.db.SelectUpcomingTrackedMedia
import com.ajinkyabadve.kmmmywatchlist.db.TrackedMedia
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private object TrackedMediaConstant {
    const val CATEGORY_FAVORITE = "favorite"
    const val CATEGORY_WATCHLIST = "watchlist"

    // TMDB's page size is fixed (not configurable via the API) - matching it exactly means each
    // RemoteMediator fetch lines up with one local Paging window instead of a partial one.
    const val PAGE_SIZE = 20
}

/** One TV show's poll-relevant state, as read by [TrackedMediaRepository.trackedTvForPolling]. */
data class TrackedTvPollCandidate(
    val id: Int,
    val lastKnownNextEpisodeAirDate: String?,
    val lastKnownStatus: String?,
)

/** Which date [UpcomingMediaItem.date] represents - a closed set, so an enum rather than a
 *  boolean/string, per code-conventions §9. */
enum class UpcomingDateKind {
    MOVIE_RELEASE,
    NEXT_EPISODE,
}

/**
 * One row of the "Upcoming" tab (future_features_checklist.md item 18) - a tracked movie not yet
 * released ([TrackedMediaRepository.observeUpcoming], [dateKind] = [UpcomingDateKind.MOVIE_RELEASE],
 * [seasonNumber]/[episodeNumber] null), or one specific unreleased episode of a tracked TV show
 * ([dateKind] = [UpcomingDateKind.NEXT_EPISODE], built at the ScreenModel layer from
 * [TrackedMediaRepository.observeTrackedTvShows] combined with cached season data - see
 * `UpcomingReleasesScreenModel`'s kdoc for why episode-level items don't come from this repository
 * directly). A show with several unreleased episodes produces one [UpcomingMediaItem] per episode,
 * not one per show.
 */
data class UpcomingMediaItem(
    val id: Int,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val date: LocalDate,
    val dateKind: UpcomingDateKind,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
)

/** One currently tracked TV show, as read by [TrackedMediaRepository.observeTrackedTvShows]. */
data class TrackedTvShowSummary(
    val id: Int,
    val title: String,
    val posterPath: String?,
)

/**
 * Local SQLite mirror of a signed-in user's favorites/watchlist (see
 * `docs/local-storage-plan.html` and `shipped_features.md` item 2) - a read-cache for
 * instant local paint plus a home for shipped_features.md item 3's per-item poll state,
 * not a source of truth. [pagedFlow]/[TrackedMediaRemoteMediator] are what keep it in sync with
 * TMDB; nothing here ever writes to TMDB itself.
 */
interface TrackedMediaRepository {
    /**
     * Paging3-backed source for the Favorites/Watchlist grid - fetches only the page the grid has
     * actually scrolled to, via [TrackedMediaRemoteMediator], and reads back through a
     * `QueryPagingSource` over the local table. One (category, mediaType) slice at a time,
     * matching the grid's Movie/TV chip - see `MyDatabase.sq`'s `selectByCategoryPaged`.
     */
    fun pagedFlow(
        category: AccountMediaCategory,
        mediaType: SearchMediaType,
        accountId: Long,
        sessionId: String,
    ): Flow<PagingData<SearchResultItem>>

    suspend fun updatePollState(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
        nextEpisodeAirDate: String?,
        creditIds: String?,
    )

    /**
     * The notification poller's candidate set: every non-deleted tracked TV show, deduplicated
     * across favorite/watchlist (a show tracked under both is only polled once) - see
     * `MyDatabase.sq`'s `selectTrackedTvForPolling` kdoc. Empty on the web target, which has no
     * local table to enumerate - see `NetworkOnlyTrackedMediaRepositoryImpl`'s kdoc.
     */
    suspend fun trackedTvForPolling(): List<TrackedTvPollCandidate>

    /** Debug-only: forgets every tracked TV show's last-seen air date, so the next poll treats its
     *  current next-episode as newly announced again - see AccountScreen's "Poll episode
     *  notifications now" row and `MyDatabase.sq`'s `resetAllTvPollStateForDebug` kdoc. */
    suspend fun resetAllTvPollStateForDebug()

    /** Poller-only write: updates every category row for this (id, mediaType) at once - see
     *  `updatePollStateForMediaType`'s kdoc in `MyDatabase.sq` for why category is deliberately
     *  not part of this call, unlike [updatePollState]. */
    suspend fun updatePollStateForMediaType(
        id: Int,
        mediaType: String,
        nextEpisodeAirDate: String?,
    )

    /** Poller-only write, same category-agnostic reasoning as [updatePollStateForMediaType]. */
    suspend fun updateLastKnownStatusForMediaType(
        id: Int,
        mediaType: String,
        status: String?,
    )

    /**
     * Hides [id]/[mediaType]/[category] from [pagedFlow] immediately and queues it for removal
     * from TMDB - call this right after a favorite/watchlist toggle turns a title off, before the
     * network call even resolves, so the local list drops it whether or not the device is online.
     */
    suspend fun markPendingDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    )

    /** Undoes [markPendingDelete] - for a toggle-back-on before the removal ever reached TMDB. */
    suspend fun clearPendingDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    )

    /** Hard-deletes a row [markPendingDelete] queued, once its removal is confirmed with TMDB. */
    suspend fun confirmDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    )

    /**
     * future_features_checklist.md item 18: every tracked movie not yet released, soonest first -
     * see `MyDatabase.sq`'s `selectUpcomingTrackedMedia` kdoc. Movie-only: a TV show's *next*
     * episode date alone can't list every unreleased episode of its current season, so TV is
     * covered by [observeTrackedTvShows] instead, combined with cached season data at the
     * ScreenModel layer. Purely local/reactive, no network call. Empty on the web target, which has
     * no local table to enumerate - see `NetworkOnlyTrackedMediaRepositoryImpl`'s kdoc.
     */
    fun observeUpcoming(): Flow<List<UpcomingMediaItem>>

    /**
     * Every currently tracked TV show, regardless of poll state - see `MyDatabase.sq`'s
     * `selectTrackedTvShows` kdoc. Used to build the "Upcoming" tab's per-episode entries from
     * cached season data (`TvDetailCacheRepository.observeSeasons`), since this repository has no
     * per-episode dates of its own. Empty on the web target - see
     * `NetworkOnlyTrackedMediaRepositoryImpl`'s kdoc.
     */
    fun observeTrackedTvShows(): Flow<List<TrackedTvShowSummary>>
}

/**
 * [TrackedMediaRepository] resolves to [SqliteTrackedMediaRepositoryImpl] everywhere except the
 * web target, which skips local storage entirely - see jsMain's `NetworkOnlyTrackedMediaRepositoryImpl`
 * kdoc for why (sql.js/WebWorkerDriver's build/bundling cost wasn't worth it for a platform with no
 * offline story to begin with). Callers never see the difference - same public class name, same
 * constructor, chosen per-platform via this `expect`/`actual` factory.
 */
internal expect fun createTrackedMediaRepository(
    accountMediaRepository: AccountMediaRepository,
    databaseProvider: suspend () -> MyDatabase,
): TrackedMediaRepository

class TrackedMediaRepositoryImpl(
    accountMediaRepository: AccountMediaRepository = AccountMediaRepositoryImpl(),
    // Test-only seam, same shape as this app's other `expect`/`actual`-backed dependencies (e.g.
    // RegionRepositoryImpl's `settings` param) - lets a test point at an isolated in-memory
    // database instead of the shared app-wide singleton. Driver creation is suspend (the JS
    // WebWorkerDriver needs to await schema creation), so this can't be a plain default value the
    // way `Settings` is - it has to stay a suspend factory.
    databaseProvider: suspend () -> MyDatabase = { AppDatabaseProvider.get() },
) : TrackedMediaRepository by createTrackedMediaRepository(accountMediaRepository, databaseProvider)

internal class SqliteTrackedMediaRepositoryImpl(
    private val accountMediaRepository: AccountMediaRepository,
    private val databaseProvider: suspend () -> MyDatabase,
) : TrackedMediaRepository {
    @OptIn(ExperimentalPagingApi::class)
    override fun pagedFlow(
        category: AccountMediaCategory,
        mediaType: SearchMediaType,
        accountId: Long,
        sessionId: String,
    ): Flow<PagingData<SearchResultItem>> =
        flow {
            // Resolved once per collector, not per pagingSourceFactory call - Pager's factory must
            // be synchronous, but AppDatabaseProvider.get() is suspend (JS's WebWorkerDriver needs
            // to await schema creation) - same reason every other method here uses databaseProvider.
            val database = databaseProvider()
            val queries = database.myDatabaseQueries
            val queryKey = "${category.storageValue}_${mediaType.apiValue}"
            val pager =
                Pager(
                    // initialLoadSize defaults to pageSize * 3, which would fetch 3 remote pages
                    // before the grid ever paints - fixing it to one page keeps the first paint
                    // to a single network call and makes append (scroll-triggered) loading, and
                    // its loader, actually visible instead of front-loading everything upfront.
                    config = PagingConfig(pageSize = TrackedMediaConstant.PAGE_SIZE, initialLoadSize = TrackedMediaConstant.PAGE_SIZE),
                    remoteMediator =
                        TrackedMediaRemoteMediator(
                            queryKey = queryKey,
                            category = category,
                            mediaType = mediaType,
                            accountId = accountId,
                            sessionId = sessionId,
                            accountMediaRepository = accountMediaRepository,
                            database = database,
                        ),
                ) {
                    QueryPagingSource(
                        countQuery = queries.countByCategory(category.storageValue, mediaType.apiValue),
                        transacter = queries,
                        context = Dispatchers.Default,
                        queryProvider = {
                            limit,
                            offset,
                            ->
                            queries.selectByCategoryPaged(category.storageValue, mediaType.apiValue, limit, offset)
                        },
                    )
                }
            emitAll(pager.flow.map { pagingData -> pagingData.map { it.toSearchResultItem() } })
        }

    override suspend fun updatePollState(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
        nextEpisodeAirDate: String?,
        creditIds: String?,
    ) {
        databaseProvider().myDatabaseQueries.updatePollState(
            lastKnownNextEpisodeAirDate = nextEpisodeAirDate,
            lastKnownCreditIds = creditIds,
            id = id.toLong(),
            mediaType = mediaType,
            category = category.storageValue,
        )
    }

    override suspend fun trackedTvForPolling(): List<TrackedTvPollCandidate> =
        databaseProvider()
            .myDatabaseQueries
            .selectTrackedTvForPolling()
            .awaitAsList()
            .map { row -> TrackedTvPollCandidate(row.id.toInt(), row.lastKnownNextEpisodeAirDate, row.lastKnownStatus) }

    override suspend fun resetAllTvPollStateForDebug() {
        databaseProvider().myDatabaseQueries.resetAllTvPollStateForDebug()
    }

    override suspend fun updatePollStateForMediaType(
        id: Int,
        mediaType: String,
        nextEpisodeAirDate: String?,
    ) {
        databaseProvider().myDatabaseQueries.updatePollStateForMediaType(nextEpisodeAirDate, id.toLong(), mediaType)
    }

    override suspend fun updateLastKnownStatusForMediaType(
        id: Int,
        mediaType: String,
        status: String?,
    ) {
        databaseProvider().myDatabaseQueries.updateLastKnownStatusForMediaType(status, id.toLong(), mediaType)
    }

    override suspend fun markPendingDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    ) {
        databaseProvider().myDatabaseQueries.markTrackedMediaPendingDelete(id.toLong(), mediaType, category.storageValue)
    }

    override suspend fun clearPendingDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    ) {
        databaseProvider().myDatabaseQueries.clearTrackedMediaPendingDelete(id.toLong(), mediaType, category.storageValue)
    }

    override suspend fun confirmDelete(
        id: Int,
        mediaType: String,
        category: AccountMediaCategory,
    ) {
        databaseProvider().myDatabaseQueries.deleteTrackedMediaRow(id.toLong(), mediaType, category.storageValue)
    }

    override fun observeUpcoming(): Flow<List<UpcomingMediaItem>> =
        flow {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectUpcomingTrackedMedia(today)
                    .asFlow()
                    .mapToList(Dispatchers.Default)
                    .map { rows -> rows.map { it.toUpcomingMediaItem() } },
            )
        }

    override fun observeTrackedTvShows(): Flow<List<TrackedTvShowSummary>> =
        flow {
            emitAll(
                databaseProvider()
                    .myDatabaseQueries
                    .selectTrackedTvShows()
                    .asFlow()
                    .mapToList(Dispatchers.Default)
                    .map { rows -> rows.map { TrackedTvShowSummary(id = it.id.toInt(), title = it.title, posterPath = it.posterPath) } },
            )
        }
}

internal val AccountMediaCategory.storageValue: String
    get() =
        when (this) {
            AccountMediaCategory.FAVORITES -> TrackedMediaConstant.CATEGORY_FAVORITE
            AccountMediaCategory.WATCHLIST -> TrackedMediaConstant.CATEGORY_WATCHLIST
        }

private fun SelectUpcomingTrackedMedia.toUpcomingMediaItem(): UpcomingMediaItem =
    UpcomingMediaItem(
        id = id.toInt(),
        mediaType = MediaTypeConstant.MOVIE,
        title = title,
        posterPath = posterPath,
        // Guaranteed non-null here: selectUpcomingTrackedMedia's WHERE clause only admits rows
        // where releaseDate is non-null and >= today - upcomingDate is only String? because it's
        // aliased in the SELECT rather than selected as the plain (non-null) column directly.
        date = LocalDate.parse(upcomingDate!!),
        dateKind = UpcomingDateKind.MOVIE_RELEASE,
    )

private fun TrackedMedia.toSearchResultItem(): SearchResultItem =
    if (mediaType == MediaTypeConstant.TV) {
        SearchResultItem(
            id = id.toInt(),
            mediaTypeRaw = mediaType,
            name = title,
            firstAirDate = releaseDate,
            posterPath = posterPath,
            voteAverage = voteAverage,
        )
    } else {
        SearchResultItem(
            id = id.toInt(),
            mediaTypeRaw = mediaType,
            title = title,
            releaseDate = releaseDate,
            posterPath = posterPath,
            voteAverage = voteAverage,
        )
    }
