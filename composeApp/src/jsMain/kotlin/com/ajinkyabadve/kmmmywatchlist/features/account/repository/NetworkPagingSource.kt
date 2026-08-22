package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.SerializationException

/**
 * Plain network-backed PagingSource for the web target, which skips the SQLite/RemoteMediator
 * mirror entirely - see [NetworkOnlyTrackedMediaRepositoryImpl]'s kdoc for why. Fetches TMDB's
 * page-number pagination directly, one page per load call, with no local cache to read back
 * through - unlike the SQLite platforms' `QueryPagingSource` + `RemoteMediator` pair, this is the
 * entire pipeline by itself.
 */
internal class NetworkPagingSource<T : Any>(
    private val fetchPage: suspend (page: Int) -> Pair<List<T>, Int?>,
) : PagingSource<Int, T>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 1
        return try {
            val (items, totalPages) = fetchPage(page)
            LoadResult.Page(
                data = items,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (totalPages != null && page < totalPages) page + 1 else null,
            )
        } catch (e: HttpExceptions) {
            LoadResult.Error(e)
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: ContentConvertException) {
            LoadResult.Error(e)
        } catch (e: SerializationException) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? = null
}
