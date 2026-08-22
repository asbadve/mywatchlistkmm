package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbList
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.CustomListRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.CustomListRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

sealed interface CreateListState {
    data object Idle : CreateListState

    data object Creating : CreateListState

    data class Error(
        val message: String,
    ) : CreateListState
}

/**
 * Paging3-backed - the signed-in user's custom lists, plus creating a new one. Pagination itself
 * (page-fetch, local upsert) now lives in [CustomListRepository.pagedFlow]/`ListsRemoteMediator` -
 * see `AccountMediaListScreenModel`'s identical kdoc for the shape this mirrors.
 */
class ListsScreenModel(
    private val accountId: Long,
    private val sessionId: String,
    private val listsRepository: ListsRepository = ListsRepositoryImpl(),
    private val customListRepository: CustomListRepository = CustomListRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    /** Collect via `collectAsLazyPagingItems()` - `.cachedIn` survives recomposition/tab switches. */
    val pagedLists: Flow<PagingData<TmdbList>> = customListRepository.pagedFlow(accountId, sessionId).cachedIn(viewModelScope)

    var createListState by mutableStateOf<CreateListState>(CreateListState.Idle)
        private set

    fun createList(
        name: String,
        description: String,
        onCreated: (listId: Long) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            createListState = CreateListState.Creating
            try {
                val listId = listsRepository.createList(sessionId, name, description)
                // Written straight to the local table so it shows up immediately - QueryPagingSource
                // auto-invalidates on this write, same reactivity every other local mutation in this
                // codebase relies on, rather than needing an explicit "refresh the grid" call.
                customListRepository.upsertListLocally(TmdbList(id = listId, name = name, description = description))
                createListState = CreateListState.Idle
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

    /**
     * Full "fetch every list" sync, for callers that need the complete list index right away rather
     * than the Paging3 grid's scroll-windowed subset - e.g. `AddToListDialog`'s picker, which needs
     * every list to search/select from, not just whichever pages [pagedLists] has fetched so far.
     * Best-effort: a failure here just leaves the local table as fresh as its last successful sync,
     * same as `AccountMediaListScreenModel`'s old `syncLocalCacheInBackground` never blocked its own
     * screen on this same kind of background reconciliation.
     */
    fun ensureAllListsAreSynced() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                customListRepository.sync(accountId, sessionId)
            } catch (e: HttpExceptions) {
                Napier.e(tag = TAG, throwable = e) { "Http error syncing the full list index" }
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "Network error syncing the full list index" }
            } catch (e: ContentConvertException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response syncing the full list index" }
            } catch (e: SerializationException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response syncing the full list index" }
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "ListsScreenModel"
    }
}
