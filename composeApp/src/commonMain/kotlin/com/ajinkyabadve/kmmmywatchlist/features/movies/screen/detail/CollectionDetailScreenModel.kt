package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.aggregateFeaturedCast
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.aggregateFeaturedCrew
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
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
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_collection

sealed interface CollectionDetailState {
    data object Loading : CollectionDetailState

    // featuredCast/featuredCrew arrive in a second pass, aggregated from every part's movie
    // credits, so the collection itself renders without waiting on them.
    data class Success(
        val collection: CollectionDetail,
        val featuredCast: List<CastMember> = emptyList(),
        val featuredCrew: List<CastMember> = emptyList(),
    ) : CollectionDetailState

    data class Error(
        val message: UiText,
    ) : CollectionDetailState
}

class CollectionDetailScreenModel(
    private val collectionId: Long,
    private val movieRepository: MovieRepository = MovieRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<CollectionDetailState>(CollectionDetailState.Loading)
    val uiState: StateFlow<CollectionDetailState> = _uiState.asStateFlow()

    init {
        loadCollectionDetails()
    }

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
                _uiState.value = CollectionDetailState.Error(UiText.Plain(httpExceptions.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) {
                    "IO/Network Error fetching collection details for collectionId: $collectionId"
                }
                _uiState.value = CollectionDetailState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: ContentConvertException) {
                logMalformedResponse(e)
                _uiState.value = CollectionDetailState.Error(UiText.Resource(Res.string.error_unexpected_collection))
            } catch (e: SerializationException) {
                logMalformedResponse(e)
                _uiState.value = CollectionDetailState.Error(UiText.Resource(Res.string.error_unexpected_collection))
            }
        }
    }

    // The collection endpoint carries no credits, so fetch every part's /movie/{id}/credits in
    // parallel and aggregate. A part failing just means it doesn't contribute - the featured
    // sections stay hidden only if nothing loads.
    private suspend fun loadFeaturedCredits(collection: CollectionDetail) {
        if (collection.parts.isEmpty()) return
        val creditsPerMovie =
            coroutineScope {
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
            _uiState.value =
                current.copy(
                    featuredCast = aggregateFeaturedCast(creditsPerMovie),
                    featuredCrew = aggregateFeaturedCrew(creditsPerMovie),
                )
        }
    }

    private fun logCreditsFailure(
        partId: Int,
        throwable: Throwable,
    ) {
        Napier.e(tag = TAG, throwable = throwable) {
            "Failed to fetch credits for part $partId of collection $collectionId"
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private fun logMalformedResponse(throwable: Throwable) {
        Napier.e(tag = TAG, throwable = throwable) {
            "Malformed response while loading collection details"
        }
    }

    private companion object {
        const val TAG = "CollectionDetailScreenModel"
    }
}
