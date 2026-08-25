package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.db.TrackedMedia
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AccountMediaCategory
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException

/**
 * Paging3's network-side half for one (category, mediaType) slice of favorites/watchlist - see
 * `TrackedMediaRepository.pagedFlow`'s kdoc for how this replaces `sync()`'s "fetch every page every
 * time" behavior with "fetch only the page the grid has actually scrolled to." [queryKey] is the
 * `remoteKeys` row this instance's next-page bookkeeping lives under (see `MyDatabase.sq`).
 */
@OptIn(ExperimentalPagingApi::class)
class TrackedMediaRemoteMediator(
    private val queryKey: String,
    private val category: AccountMediaCategory,
    private val mediaType: SearchMediaType,
    private val accountId: Long,
    private val sessionId: String,
    private val accountMediaRepository: AccountMediaRepository,
    private val database: MyDatabase,
) : RemoteMediator<Int, TrackedMedia>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TrackedMedia>,
    ): MediatorResult =
        // Dispatchers.IO is JVM/Android-only, not part of kotlinx.coroutines' multiplatform
        // Dispatchers API - Default is this codebase's established stand-in everywhere else
        // (see every asFlow()/mapToList(Dispatchers.Default) call in the repository layer).
        withContext(Dispatchers.Default) {
            try {
                val page =
                    when (loadType) {
                        LoadType.REFRESH -> 1
                        LoadType.PREPEND -> return@withContext MediatorResult.Success(endOfPaginationReached = true)
                        LoadType.APPEND -> {
                            val keys = database.myDatabaseQueries.selectRemoteKeys(queryKey).awaitAsOneOrNull()
                            keys?.nextPage?.toInt() ?: return@withContext MediatorResult.Success(endOfPaginationReached = true)
                        }
                    }
                val response = fetchPage(page)
                val queries = database.myDatabaseQueries
                val now = Clock.System.now().toEpochMilliseconds()
                if (loadType == LoadType.REFRESH) {
                    // Only clears rows with no queued offline delete - same pendingSync guard
                    // deleteTrackedMediaNotIn already uses for the non-paginated sync() path, so a
                    // refresh mid-offline-queue can't silently drop a delete that hasn't flushed yet.
                    queries.deleteTrackedMediaPagedRefresh(category.storageValue, mediaType.apiValue)
                }
                response.list.orEmpty().forEach { item ->
                    queries.upsertTrackedMedia(
                        id = item.id.toLong(),
                        mediaType = mediaType.apiValue,
                        category = category.storageValue,
                        title = item.displayTitle,
                        posterPath = item.imagePath,
                        releaseDate = item.releaseDate ?: item.firstAirDate,
                        voteAverage = item.voteAverage,
                        addedAt = now,
                        lastSyncedAt = now,
                    )
                }
                val totalPages = response.totalPages ?: page
                queries.upsertRemoteKeys(
                    queryKey = queryKey,
                    nextPage = (page + 1).toLong().takeIf { page < totalPages },
                    prevPage = null,
                    nextKey = null,
                )
                MediatorResult.Success(endOfPaginationReached = page >= totalPages)
            } catch (e: HttpExceptions) {
                MediatorResult.Error(e)
            } catch (e: IOException) {
                MediatorResult.Error(e)
            } catch (e: ContentConvertException) {
                MediatorResult.Error(e)
            } catch (e: SerializationException) {
                MediatorResult.Error(e)
            }
        }

    private suspend fun fetchPage(page: Int) =
        when (category) {
            AccountMediaCategory.FAVORITES ->
                when (mediaType) {
                    SearchMediaType.MOVIE -> accountMediaRepository.getFavoriteMovies(accountId, sessionId, page)
                    SearchMediaType.TV -> accountMediaRepository.getFavoriteTv(accountId, sessionId, page)
                    SearchMediaType.PERSON -> error("Favorites has no person media type")
                }

            AccountMediaCategory.WATCHLIST ->
                when (mediaType) {
                    SearchMediaType.MOVIE -> accountMediaRepository.getWatchlistMovies(accountId, sessionId, page)
                    SearchMediaType.TV -> accountMediaRepository.getWatchlistTv(accountId, sessionId, page)
                    SearchMediaType.PERSON -> error("Watchlist has no person media type")
                }
        }
}
