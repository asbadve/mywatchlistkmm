package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.account.model.TmdbListDetail
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
        uiState = ListDetailState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val detail = listsRepository.getListDetails(listId, sessionId)
                uiState = ListDetailState.Success(detail)
            } catch (e: HttpExceptions) {
                Napier.e(tag = TAG, throwable = e) { "Http error loading list $listId" }
                uiState = ListDetailState.Error(UiText.Plain(e.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "Network error loading list $listId" }
                uiState = ListDetailState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: ContentConvertException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response loading list $listId" }
                uiState = ListDetailState.Error(UiText.Resource(Res.string.error_unexpected_list))
            } catch (e: SerializationException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response loading list $listId" }
                uiState = ListDetailState.Error(UiText.Resource(Res.string.error_unexpected_list))
            }
        }
    }

    fun removeMovie(movieId: Long) {
        val current = uiState
        if (current !is ListDetailState.Success) return
        viewModelScope.launch(Dispatchers.Main) {
            try {
                listsRepository.removeMovieFromList(listId, sessionId, movieId)
                uiState = ListDetailState.Success(current.detail.copy(items = current.detail.items.filter { it.id.toLong() != movieId }))
            } catch (e: HttpExceptions) {
                Napier.e(tag = TAG, throwable = e) { "Http error removing movie $movieId from list $listId" }
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "Network error removing movie $movieId from list $listId" }
            } catch (e: ContentConvertException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response removing movie $movieId from list $listId" }
            } catch (e: SerializationException) {
                Napier.e(tag = TAG, throwable = e) { "Malformed response removing movie $movieId from list $listId" }
            }
        }
    }

    fun deleteList() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                listsRepository.deleteList(listId, sessionId)
                isDeleted = true
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
