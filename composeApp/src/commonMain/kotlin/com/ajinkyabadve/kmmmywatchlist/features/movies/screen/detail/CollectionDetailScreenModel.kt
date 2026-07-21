package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.aggregateFeaturedCast
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.aggregateFeaturedCrew
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

sealed interface CollectionDetailState {
    data object Loading : CollectionDetailState

    // featuredCast/featuredCrew arrive in a second pass, aggregated from every part's movie
    // credits, so the collection itself renders without waiting on them.
    data class Success(
        val collection: CollectionDetail,
        val featuredCast: List<CastMember> = emptyList(),
        val featuredCrew: List<CastMember> = emptyList(),
    ) : CollectionDetailState

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
                loadFeaturedCredits(collection)
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = TAG, throwable = httpExceptions) {
                    "HTTP Error fetching collection details for collectionId: $collectionId"
                }
                _uiState.value = CollectionDetailState.Error(httpExceptions.message)
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) {
                    "IO/Network Error fetching collection details for collectionId: $collectionId"
                }
                _uiState.value = CollectionDetailState.Error("Network Connection Error. Please check your internet connectivity.")
            } catch (e: Exception) {
                Napier.e(tag = TAG, throwable = e) {
                    "Unexpected Error fetching collection details for collectionId: $collectionId"
                }
                _uiState.value = CollectionDetailState.Error("An unexpected error occurred while loading the collection. Please try again.")
            }
        }
    }

    // The collection endpoint carries no credits, so fetch every part's /movie/{id}/credits in
    // parallel and aggregate. A part failing just means it doesn't contribute - the featured
    // sections stay hidden only if nothing loads.
    private suspend fun loadFeaturedCredits(collection: CollectionDetail) {
        if (collection.parts.isEmpty()) return
        val creditsPerMovie = coroutineScope {
            collection.parts
                .map { part ->
                    async {
                        try {
                            movieRepository.getMovieCredits(part.id.toLong())
                        } catch (e: HttpExceptions) {
                            logCreditsFailure(part.id, e)
                            null
                        } catch (e: IOException) {
                            logCreditsFailure(part.id, e)
                            null
                        } catch (e: SerializationException) {
                            logCreditsFailure(part.id, e)
                            null
                        }
                    }
                }.awaitAll()
                .filterNotNull()
        }
        if (creditsPerMovie.isEmpty()) return
        val current = _uiState.value
        if (current is CollectionDetailState.Success && current.collection.id == collection.id) {
            _uiState.value = current.copy(
                featuredCast = aggregateFeaturedCast(creditsPerMovie),
                featuredCrew = aggregateFeaturedCrew(creditsPerMovie),
            )
        }
    }

    private fun logCreditsFailure(partId: Int, throwable: Throwable) {
        Napier.e(tag = TAG, throwable = throwable) {
            "Failed to fetch credits for part $partId of collection $collectionId"
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "CollectionDetailScreenModel"
    }
}
