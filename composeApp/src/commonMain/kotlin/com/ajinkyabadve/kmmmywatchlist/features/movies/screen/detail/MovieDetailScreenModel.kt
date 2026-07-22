package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.error_network
import mywatchlist.composeapp.generated.resources.error_unexpected_movie_details

sealed interface MovieDetailState {
    data object Loading : MovieDetailState

    data class Success(
        val movieDetail: MovieDetail,
    ) : MovieDetailState

    data class Error(
        val message: UiText,
    ) : MovieDetailState
}

class MovieDetailScreenModel(
    private val movieId: Long,
    private val movieRepository: MovieRepository = MovieRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<MovieDetailState>(MovieDetailState.Loading)
    val uiState: StateFlow<MovieDetailState> = _uiState.asStateFlow()

    init {
        loadMovieDetails()
    }

    fun loadMovieDetails() {
        _uiState.value = MovieDetailState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val detail = movieRepository.getMovieDetails(movieId)
                _uiState.value = MovieDetailState.Success(detail)
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = TAG, throwable = httpExceptions) { "HTTP Error fetching details for movieId: $movieId" }
                _uiState.value = MovieDetailState.Error(UiText.Plain(httpExceptions.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "IO/Network Error fetching details for movieId: $movieId" }
                _uiState.value = MovieDetailState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: ContentConvertException) {
                logMalformedResponse(e)
                _uiState.value = MovieDetailState.Error(UiText.Resource(Res.string.error_unexpected_movie_details))
            } catch (e: SerializationException) {
                logMalformedResponse(e)
                _uiState.value = MovieDetailState.Error(UiText.Resource(Res.string.error_unexpected_movie_details))
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    private fun logMalformedResponse(throwable: Throwable) {
        Napier.e(tag = TAG, throwable = throwable) {
            "Malformed response while loading movie details"
        }
    }

    private companion object {
        const val TAG = "MovieDetailScreenModel"
    }
}
