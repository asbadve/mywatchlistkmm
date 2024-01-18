package com.ajinkyabadve.kmmmywatchlist.features.nowplaying

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.ajinkyabadve.kmmmywatchlist.features.nowplaying.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.nowplaying.repository.NowPlayingRepositoryImpl

class NowPlayingMoviesViewModel {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private val nowPlayingRepository = NowPlayingRepositoryImpl()
    val state = MutableStateFlow<MovieListScreenState>(MovieListScreenState.Loading)

    init {
        viewModelScope.launch(Dispatchers.Main) {
            state.emit(MovieListScreenState.Loading)
            try {
                val response = nowPlayingRepository.getNowPlayingMovies(1)
                response.list?.let {
                    state.emit(
                        MovieListScreenState.Success(
                            it,
                        ),
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                state.emit(MovieListScreenState.Error(e.message.toString()))
            }
        }
    }
}

sealed interface MovieListScreenState {
    data object Loading : MovieListScreenState
    data class Error(val message: String) : MovieListScreenState
    data class Success(
        val countriesList: List<Movie>,
    ) : MovieListScreenState
}
