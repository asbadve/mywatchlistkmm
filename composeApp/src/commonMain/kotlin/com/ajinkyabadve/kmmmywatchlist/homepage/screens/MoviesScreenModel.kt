package com.ajinkyabadve.kmmmywatchlist.homepage.screens

import cafe.adriel.voyager.core.model.ScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.nowplaying.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.nowplaying.repository.NowPlayingRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MoviesScreenModel(movieFetchType: String = "now_playing") : ScreenModel {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private val nowPlayingRepository = NowPlayingRepositoryImpl()
    val state = MutableStateFlow<MovieListScreenState>(MovieListScreenState.Loading)

    init {
        viewModelScope.launch(Dispatchers.Main) {
            state.emit(MovieListScreenState.Loading)
            try {
                val response = nowPlayingRepository.getNowPlayingMovies(1, movieFetchType)
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

    override fun onDispose() {
        super.onDispose()
    }
}

sealed interface MovieListScreenState {
    data object Loading : MovieListScreenState
    data class Error(val message: String) : MovieListScreenState
    data class Success(
        val countriesList: List<Movie>,
    ) : MovieListScreenState
}
