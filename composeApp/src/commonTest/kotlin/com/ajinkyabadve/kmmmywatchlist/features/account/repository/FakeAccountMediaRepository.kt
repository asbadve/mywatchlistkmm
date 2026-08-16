package com.ajinkyabadve.kmmmywatchlist.features.account.repository

import com.ajinkyabadve.kmmmywatchlist.features.account.model.AccountStates
import com.ajinkyabadve.kmmmywatchlist.features.account.model.StatusResponse
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import io.ktor.utils.io.errors.IOException

class FakeAccountMediaRepository : AccountMediaRepository {
    var favoriteMoviesResult: Result<SearchPageResult>? = null
    var favoriteTvResult: Result<SearchPageResult>? = null
    var watchlistMoviesResult: Result<SearchPageResult>? = null
    var watchlistTvResult: Result<SearchPageResult>? = null
    var setFavoriteResult: Result<StatusResponse>? = null
    var setWatchlistResult: Result<StatusResponse>? = null
    var accountStatesResult: Result<AccountStates>? = null

    val setFavoriteCalls = mutableListOf<Boolean>()
    val setWatchlistCalls = mutableListOf<Boolean>()

    private fun emptyPage() = SearchPageResult(page = 1, list = emptyList(), totalPages = 1)

    private fun <T> Result<T>?.orDefault(default: T): T =
        this?.let { result ->
            if (result.isSuccess) result.getOrThrow() else throw result.exceptionOrNull() ?: IOException("Fake repository error")
        } ?: default

    override suspend fun getFavoriteMovies(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult = favoriteMoviesResult.orDefault(emptyPage())

    override suspend fun getFavoriteTv(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult = favoriteTvResult.orDefault(emptyPage())

    override suspend fun getWatchlistMovies(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult = watchlistMoviesResult.orDefault(emptyPage())

    override suspend fun getWatchlistTv(
        accountId: Long,
        sessionId: String,
        page: Int,
    ): SearchPageResult = watchlistTvResult.orDefault(emptyPage())

    override suspend fun setFavorite(
        accountId: Long,
        sessionId: String,
        mediaType: String,
        mediaId: Long,
        favorite: Boolean,
    ): StatusResponse {
        setFavoriteCalls.add(favorite)
        return setFavoriteResult.orDefault(StatusResponse(statusCode = 1, statusMessage = "Success."))
    }

    override suspend fun setWatchlist(
        accountId: Long,
        sessionId: String,
        mediaType: String,
        mediaId: Long,
        watchlist: Boolean,
    ): StatusResponse {
        setWatchlistCalls.add(watchlist)
        return setWatchlistResult.orDefault(StatusResponse(statusCode = 1, statusMessage = "Success."))
    }

    override suspend fun getAccountStates(
        sessionId: String,
        mediaType: String,
        mediaId: Long,
    ): AccountStates = accountStatesResult.orDefault(AccountStates())
}
