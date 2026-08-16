package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import com.ajinkyabadve.kmmmywatchlist.network.isServerError
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

sealed interface CreateListState {
    data object Idle : CreateListState

    data object Creating : CreateListState

    data class Error(
        val message: String,
    ) : CreateListState
}

/** Paginates the signed-in user's custom lists and handles creating a new one. */
class ListsScreenModel(
    private val accountId: Long,
    private val sessionId: String,
    private val listsRepository: ListsRepository = ListsRepositoryImpl(),
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    internal val lists = mutableStateListOf<TmdbList>()

    private var page by mutableStateOf(1)
    private var canPaginate by mutableStateOf(false)
    var listState by mutableStateOf(ListState.IDLE)
    var createListState by mutableStateOf<CreateListState>(CreateListState.Idle)
        private set

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
                    val response = listsRepository.getLists(accountId, sessionId, page)
                    response.list?.let { pageLists ->
                        canPaginate = page < (response.totalPages ?: page)
                        if (isFirstPage()) {
                            lists.clear()
                        }
                        lists.addAll(pageLists)
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

    fun createList(
        name: String,
        description: String,
        onCreated: (listId: Long) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            createListState = CreateListState.Creating
            try {
                val listId = listsRepository.createList(sessionId, name, description)
                createListState = CreateListState.Idle
                refresh()
                onCreated(listId)
            } catch (e: HttpExceptions) {
                createListState = CreateListState.Error(e.message.orEmpty())
            } catch (e: IOException) {
                createListState = CreateListState.Error(e.message.orEmpty())
            } catch (e: ContentConvertException) {
                createListState = CreateListState.Error(e.message.orEmpty())
            } catch (e: SerializationException) {
                createListState = CreateListState.Error(e.message.orEmpty())
            }
        }
    }

    fun resetCreateListState() {
        createListState = CreateListState.Idle
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
