package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CollectionDetailState {
    data object Loading : CollectionDetailState
    data class Success(val collection: CollectionDetail) : CollectionDetailState
    data class Error(val message: String) : CollectionDetailState
}

class CollectionDetailScreenModel(
    private val collectionId: Long,
    private val movieRepository: MovieRepository = MovieRepositoryImpl()
) : ViewModel() {

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<CollectionDetailState>(CollectionDetailState.Loading)
    val uiState: StateFlow<CollectionDetailState> = _uiState.asStateFlow()

    init {
        loadCollectionDetails()
    }

    @Suppress("detekt:TooGenericExceptionCaught")
    fun loadCollectionDetails() {
        _uiState.value = CollectionDetailState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val collection = movieRepository.getCollectionDetails(collectionId)
                _uiState.value = CollectionDetailState.Success(collection)
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = "CollectionDetailScreenModel", throwable = httpExceptions) {
                    "HTTP Error fetching collection details for collectionId: $collectionId"
                }
                _uiState.value = CollectionDetailState.Error(httpExceptions.message)
            } catch (e: IOException) {
                Napier.e(tag = "CollectionDetailScreenModel", throwable = e) {
                    "IO/Network Error fetching collection details for collectionId: $collectionId"
                }
                _uiState.value = CollectionDetailState.Error("Network Connection Error. Please check your internet connectivity.")
            } catch (e: Exception) {
                Napier.e(tag = "CollectionDetailScreenModel", throwable = e) {
                    "Unexpected Error fetching collection details for collectionId: $collectionId"
                }
                _uiState.value = CollectionDetailState.Error("An unexpected error occurred while loading the collection. Please try again.")
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }
}
