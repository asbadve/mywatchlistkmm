package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
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
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_list

sealed interface ListDetailState {
    data object Loading : ListDetailState

    data class Success(
        val detail: TmdbListDetail,
    ) : ListDetailState

    data class Error(
        val message: UiText,
    ) : ListDetailState
}

class ListDetailScreenModel(
    private val listId: Long,
    private val sessionId: String,
    private val listsRepository: ListsRepository = ListsRepositoryImpl(),
    private val customListRepository: CustomListRepository = CustomListRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    var uiState by mutableStateOf<ListDetailState>(ListDetailState.Loading)
        private set

    var isDeleted by mutableStateOf(false)
        private set

    init {
        loadListDetails()
    }

    fun loadListDetails() {
        // CustomListRepository is the single source of truth: it decides cache-vs-network and
        // writes fresh data through to the DB - this only renders whatever it emits, and separately
        // triggers a refresh. See CustomListRepository's kdoc.
        viewModelScope.launch(Dispatchers.Main) {
            customListRepository.observeListDetail(listId).collect { detail ->
                if (detail != null) uiState = ListDetailState.Success(detail)
            }
        }
        viewModelScope.launch(Dispatchers.Main) {
            try {
                customListRepository.refreshListDetail(listId, sessionId)
            } catch (e: HttpExceptions) {
                Napier.e(tag = TAG, throwable = e) { "Http error loading list $listId" }
                if (uiState !is ListDetailState.Success) uiState = ListDetailState.Error(UiText.Plain(e.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "Network error loading list $listId" }
                if (uiState !is ListDetailState.Success) uiState = ListDetailState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: ContentConvertException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response loading list $listId" }
                if (uiState !is ListDetailState.Success) uiState = ListDetailState.Error(UiText.Resource(Res.string.error_unexpected_list))
            } catch (e: SerializationException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response loading list $listId" }
                if (uiState !is ListDetailState.Success) uiState = ListDetailState.Error(UiText.Resource(Res.string.error_unexpected_list))
            }
        }
    }

    fun removeMovie(movieId: Long) {
        if (uiState !is ListDetailState.Success) return
        viewModelScope.launch(Dispatchers.Main) {
            // Optimistic - hides the item right away regardless of network state, mirroring
            // MediaActionsState's favorite/watchlist toggle. This is a local DB write, so
            // observeListDetail()'s Flow (collected in loadListDetails()) picks it up on its own -
            // no manual uiState mutation needed here, including on rollback below.
            customListRepository.markItemPendingDelete(listId, movieId)
            try {
                listsRepository.removeMovieFromList(listId, sessionId, movieId)
                customListRepository.confirmItemDelete(listId, movieId)
            } catch (e: HttpExceptions) {
                Napier.e(tag = TAG, throwable = e) { "Http error removing movie $movieId from list $listId" }
                customListRepository.clearItemPendingDelete(listId, movieId)
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "Network error removing movie $movieId from list $listId - queued for next sync" }
            } catch (e: ContentConvertException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response removing movie $movieId from list $listId" }
                customListRepository.clearItemPendingDelete(listId, movieId)
            } catch (e: SerializationException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response removing movie $movieId from list $listId" }
                customListRepository.clearItemPendingDelete(listId, movieId)
            }
        }
    }

    fun deleteList() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                listsRepository.deleteList(listId, sessionId)
                isDeleted = true
                // Best-effort - the local cache still self-heals on the list grid's next sync()
                // if this fails, this just skips the wait for that to happen.
                customListRepository.removeListLocally(listId)
            } catch (e: HttpExceptions) {
                Napier.e(tag = TAG, throwable = e) { "Http error deleting list $listId" }
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "Network error deleting list $listId" }
            } catch (e: ContentConvertException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response deleting list $listId" }
            } catch (e: SerializationException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response deleting list $listId" }
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "ListDetailScreenModel"
    }
}
