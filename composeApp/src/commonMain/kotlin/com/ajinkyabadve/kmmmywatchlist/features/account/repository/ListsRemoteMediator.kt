package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.ajinkyabadve.kmmmywatchlist.db.CustomList
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException

private object ListsRemoteMediatorConstant {
    const val QUERY_KEY = "lists"
}

/**
 * Paging3's network-side half for the signed-in user's custom lists - same shape as
 * `TrackedMediaRemoteMediator`, see its kdoc; replaces `CustomListRepository.sync`'s "fetch every
 * page every time" with "fetch only the page the grid has actually scrolled to."
 */
@OptIn(ExperimentalPagingApi::class)
class ListsRemoteMediator(
    private val accountId: Long,
    private val sessionId: String,
    private val listsRepository: ListsRepository,
    private val database: MyDatabase,
) : RemoteMediator<Int, CustomList>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CustomList>,
    ): MediatorResult =
        // Dispatchers.IO is JVM/Android-only - see TrackedMediaRemoteMediator's identical comment.
        withContext(Dispatchers.Default) {
            try {
                val page =
                    when (loadType) {
                        LoadType.REFRESH -> 1
                        LoadType.PREPEND -> return@withContext MediatorResult.Success(endOfPaginationReached = true)
                        LoadType.APPEND -> {
                            val keys = database.myDatabaseQueries.selectRemoteKeys(ListsRemoteMediatorConstant.QUERY_KEY).awaitAsOneOrNull()
                            keys?.nextPage?.toInt() ?: return@withContext MediatorResult.Success(endOfPaginationReached = true)
                        }
                    }
                val response = listsRepository.getLists(accountId, sessionId, page)
                val queries = database.myDatabaseQueries
                val now = Clock.System.now().toEpochMilliseconds()
                if (loadType == LoadType.REFRESH) {
                    queries.deleteAllLists()
                }
                response.list.orEmpty().forEach { list ->
                    queries.upsertList(
                        id = list.id,
                        name = list.name,
                        description = list.description,
                        itemCount = list.itemCount.toLong(),
                        posterPath = list.posterPath,
                        lastSyncedAt = now,
                    )
                }
                val totalPages = response.totalPages ?: page
                queries.upsertRemoteKeys(
                    queryKey = ListsRemoteMediatorConstant.QUERY_KEY,
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
}
