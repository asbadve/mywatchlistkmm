package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import com.ajinkyabadve.kmmmywatchlist.features.account.model.StatusResponse
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListPageResult
import io.ktor.utils.io.errors.IOException

class FakeListsRepository : ListsRepository {
    var listsResult: Result<TmdbListPageResult>? = null
    var listDetailsResult: Result<TmdbListDetail>? = null
    var createListResult: Result<Long>? = null
    var removeMovieFromListResult: Result<StatusResponse>? = null
    var deleteListResult: Result<StatusResponse>? = null

    val removeMovieFromListCalls = mutableListOf<Long>()
    var deleteListCalled = false

    private fun <T> Result<T>?.orDefault(default: T): T =
        this?.let { result ->
            if (result.isSuccess) result.getOrThrow() else throw result.exceptionOrNull() ?: IOException("Fake repository error")
        } ?: default

    override suspend fun getLists(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): TmdbListPageResult = listsResult.orDefault(TmdbListPageResult(page = 1, list = emptyList(), totalPages = 1))

    override suspend fun getListDetails(
        listId: Long,
        sessionId: String,
    ): TmdbListDetail = listDetailsResult.orDefault(TmdbListDetail(name = "Fake List", description = "", items = emptyList()))

    override suspend fun createList(
        sessionId: String,
        name: String,
        description: String,
    ): Long = createListResult.orDefault(999L)

    override suspend fun addMovieToList(
        listId: Long,
        sessionId: String,
        movieId: Long,
    ): StatusResponse = StatusResponse(statusCode = 12, statusMessage = "Updated.")

    override suspend fun removeMovieFromList(
        listId: Long,
        sessionId: String,
        movieId: Long,
    ): StatusResponse {
        removeMovieFromListCalls.add(movieId)
        return removeMovieFromListResult.orDefault(StatusResponse(statusCode = 13, statusMessage = "Deleted."))
    }

    override suspend fun deleteList(
        listId: Long,
        sessionId: String,
    ): StatusResponse {
        deleteListCalled = true
        return deleteListResult.orDefault(StatusResponse(statusCode = 12, statusMessage = "Updated."))
    }
}
