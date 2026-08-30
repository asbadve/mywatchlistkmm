package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.aggregateFeaturedCast
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.aggregateFeaturedCrew
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepositoryImpl
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
import kotlinx.coroutines.flow.Flow
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
    private val favoriteCollectionRepository: FavoriteCollectionRepository = FavoriteCollectionRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<CollectionDetailState>(CollectionDetailState.Loading)
    val uiState: StateFlow<CollectionDetailState> = _uiState.asStateFlow()

    /** Local-only "favorite collection" state - see [FavoriteCollectionRepository]'s kdoc for why
     *  this has no `account_states`-style pre-check, unlike movie/TV favorites.
     *  `FollowCollectionButton` collects this directly rather than the composable ever touching
     *  [favoriteCollectionRepository] itself, per code-conventions §6/§8. */
    val isFollowingCollection: Flow<Boolean> = favoriteCollectionRepository.observeIsFavorite(collectionId)

    init {
        loadCollectionDetails()
    }

    /** [toggleFollowCollection] returns whether this call just turned following ON (`true`) - same
     *  reasoning as [com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail.PersonDetailScreenModel.toggleFollowPerson]:
     *  `viewModel(key = "CollectionDetailScreenModel:$collectionId")` can outlive a single screen
     *  visit (this app's `NavDisplay` has no per-entry `ViewModelStore` scoping), so a
     *  ViewModel-owned flag risks surfacing the notification opt-in prompt on a later, click-free
     *  visit instead of only right after the tap that set it. */
    fun toggleFollowCollection(
        collection: CollectionDetail,
        currentlyFollowing: Boolean,
    ): Boolean {
        val newValue = !currentlyFollowing
        viewModelScope.launch {
            favoriteCollectionRepository.setFavorite(collectionId, collection.name, collection.posterPath, newValue)
        }
        return newValue
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
