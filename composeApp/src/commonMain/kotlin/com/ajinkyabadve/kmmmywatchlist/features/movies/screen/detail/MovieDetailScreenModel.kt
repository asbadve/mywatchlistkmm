package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
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

sealed interface MovieDetailState {
    data object Loading : MovieDetailState
    data class Success(val movieDetail: MovieDetail) : MovieDetailState
    data class Error(val message: String) : MovieDetailState
}

class MovieDetailScreenModel(
    private val movieId: Long,
    private val movieRepository: MovieRepository = MovieRepositoryImpl()
) : ViewModel() {

    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    
    private val _uiState = MutableStateFlow<MovieDetailState>(MovieDetailState.Loading)
    val uiState: StateFlow<MovieDetailState> = _uiState.asStateFlow()

    init {
        loadMovieDetails()
    }

    @Suppress("detekt:TooGenericExceptionCaught")
    fun loadMovieDetails() {
        _uiState.value = MovieDetailState.Loading
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val detail = movieRepository.getMovieDetails(movieId)
                _uiState.value = MovieDetailState.Success(detail)
            } catch (httpExceptions: HttpExceptions) {
                Napier.e(tag = "MovieDetailScreenModel", throwable = httpExceptions) { "HTTP Error fetching details for movieId: $movieId" }
                _uiState.value = MovieDetailState.Error(httpExceptions.message)
            } catch (e: IOException) {
                Napier.e(tag = "MovieDetailScreenModel", throwable = e) { "IO/Network Error fetching details for movieId: $movieId" }
                _uiState.value = MovieDetailState.Error("Network Connection Error. Please check your internet connectivity.")
            } catch (e: Exception) {
                Napier.e(tag = "MovieDetailScreenModel", throwable = e) { "Unexpected Error fetching details for movieId: $movieId" }
                _uiState.value = MovieDetailState.Error("An unexpected error occurred while loading movie details. Please try again.")
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }
}
