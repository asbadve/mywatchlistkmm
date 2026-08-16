package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.AccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.AccountMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchPageResult
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import com.ajinkyabadve.kmmmywatchlist.network.isServerError
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

enum class AccountMediaCategory {
    FAVORITES,
    WATCHLIST,
}

/**
 * Paginates one (category, mediaType) slice of the signed-in user's favorites/watchlist - mirrors
 * `MovieListScreenModel`'s shape (same [ListState] machine, same page/canPaginate bookkeeping) so
 * the two read the same way, but fetches [SearchResultItem]s instead of `Movie`s since a favorites/
 * watchlist grid can hold movies or TV shows depending on which chip is selected - see
 * `AccountMediaRepository`'s kdoc for why the TMDB response already fits that shape. [mediaType]
 * must be [SearchMediaType.MOVIE] or [SearchMediaType.TV] - the account-media endpoints have no
 * concept of a person result, so callers (the Favorites/Watchlist chip rows) never offer that
 * option.
 */
class AccountMediaListScreenModel(
    private val category: AccountMediaCategory,
    private val mediaType: SearchMediaType,
    private val accountId: Long,
    private val sessionId: String,
    private val accountMediaRepository: AccountMediaRepository = AccountMediaRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    internal val items = mutableStateListOf<SearchResultItem>()

    private var page by mutableStateOf(1)
    private var canPaginate by mutableStateOf(false)
    var listState by mutableStateOf(ListState.IDLE)

    init {
        load()
    }

    /** Pull-to-refresh: discards pagination progress and re-fetches page one from scratch. */
    fun refresh() {
        page = 1
        canPaginate = false
        load()
    }

    internal fun load() {
        viewModelScope.launch(Dispatchers.Main) {
            if (isFirstPage() || (isNotFirstPageAndCanPaginate() && isListStateLoadable())) {
                listState = if (isFirstPage()) ListState.LOADING else ListState.PAGINATING
                try {
                    val response = fetchPage(page)
                    response.list?.let { pageItems ->
                        canPaginate = page < (response.totalPages ?: page)
                        if (isFirstPage()) {
                            items.clear()
                        }
                        items.addAll(pageItems)
                    }
                    listState =
                        if (canPaginate) {
                            page++
                            ListState.IDLE
                        } else {
                            ListState.PAGINATION_EXHAUST
                        }
                } catch (e: HttpExceptions) {
                    if (e.response.status.value == UNAUTHORIZED_STATUS) {
                        authRepository.notifySessionExpired()
                    }
                    listState = if (e.isServerError()) ListState.NETWORK_ERROR else ListState.ERROR
                } catch (e: IOException) {
                    listState = ListState.NETWORK_ERROR
                } catch (e: ContentConvertException) {
                    listState = ListState.ERROR
                } catch (e: SerializationException) {
                    listState = ListState.ERROR
                }
            }
        }
    }

    private suspend fun fetchPage(page: Int): SearchPageResult =
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

    private fun isListStateLoadable() = listState == ListState.IDLE || listState == ListState.NETWORK_ERROR

    private fun isNotFirstPageAndCanPaginate() = page != 1 && canPaginate

    private fun isFirstPage() = page == 1

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private companion object {
        const val UNAUTHORIZED_STATUS = 401
    }
}
