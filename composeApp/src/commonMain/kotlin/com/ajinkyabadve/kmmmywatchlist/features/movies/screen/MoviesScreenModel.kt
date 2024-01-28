package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import cafe.adriel.voyager.core.model.ScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MoviesScreenModel(movieFetchType: String = NOW_PLAYING_API_PATH) : ScreenModel {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private val movieRepository = MovieRepositoryImpl()
    internal val movieState = MutableStateFlow<MovieListScreenState>(MovieListScreenState.Loading)
    internal val movieFilterState = MutableStateFlow<MovieFilterState>(
        MovieFilterState.Success(
            selectedChip = 0,
            chipItemList = chipList
        )
    )

    init {
        loadMovies(movieFetchType)
    }

    private fun loadMovies(movieFetchType: String) {
        viewModelScope.launch(Dispatchers.Main) {
            movieState.emit(MovieListScreenState.Loading)
            try {
                val response = movieRepository.getMovies(1, movieFetchType)
                response.list?.let {
                    movieState.emit(
                        MovieListScreenState.Success(
                            movieList = it,
                        ),
                    )
                }
            } catch (e: Exception) {// TODO find another solution
                e.printStackTrace()
                movieState.emit(MovieListScreenState.Error(e.message.toString()))
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
    }

    fun onChipSelected(selectedChipIndex: Int) {
        movieFilterState.update {
            MovieFilterState.Success(selectedChipIndex, chipList)
        }
        loadMovies(getMovieFetchTypeByChipTitle(selectedChipIndex))
    }

    private companion object {
        const val NOW_PLAYING_MOVIES = "Now Playing"
        const val UPCOMING_MOVIES = "Upcoming"
        const val POPULAR_MOVIES = "Popular"
        const val TOP_RATED_MOVIES = "Top Rated"

        const val NOW_PLAYING_API_PATH = "now_playing"
        const val UPCOMING_API_PATH = "upcoming"
        const val POPULAR_API_PATH = "popular"
        const val TOP_RATED_API_PATH = "top_rated"

        val chipList = listOf(
            NOW_PLAYING_MOVIES,
            UPCOMING_MOVIES,
            POPULAR_MOVIES,
            TOP_RATED_MOVIES
        )

        fun getChipTitleByIndex(index: Int): String {
            return chipList[index]
        }

        fun getMovieFetchTypeByChipTitle(index: Int): String {
            return when (getChipTitleByIndex(index)) {
                NOW_PLAYING_MOVIES -> NOW_PLAYING_API_PATH
                UPCOMING_MOVIES -> UPCOMING_API_PATH
                POPULAR_MOVIES -> POPULAR_API_PATH
                TOP_RATED_MOVIES -> TOP_RATED_API_PATH
                else -> {
                    NOW_PLAYING_API_PATH
                }
            }

        }
    }
}

internal sealed interface MovieFilterState {
    data class Success(
        val selectedChip: Int = 0,
        val chipItemList: List<String> = listOf()
    ) : MovieFilterState
}

internal sealed interface MovieListScreenState {
    data object Loading : MovieListScreenState
    data class Error(val message: String) : MovieListScreenState
    data class Success(
        val movieList: List<Movie> = listOf(),
    ) : MovieListScreenState
}
