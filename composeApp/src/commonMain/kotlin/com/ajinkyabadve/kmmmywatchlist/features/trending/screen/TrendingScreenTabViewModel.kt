package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.MEDIA_TYPE_MOVIE
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.MEDIA_TYPE_PEOPLE
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.MEDIA_TYPE_TV
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.TIME_WINDOW_DAY
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.TIME_WINDOW_WEEK
import com.ajinkyabadve.kmmmywatchlist.features.trending.TrendingConstant.trendingChipList
import com.ajinkyabadve.kmmmywatchlist.features.trending.repository.TrendingRepositoryImpl
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
@OptIn(ExperimentalSerializationApi::class)
class TrendingScreenTabViewModel(
    private val trendingRepository: TrendingRepositoryImpl = TrendingRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _isScreenLoading = MutableStateFlow(false)
    val isScreenLoading = _isScreenLoading

    private val _isMovieTrendScreenLoading = MutableStateFlow(false)
    val isMovieTrendScreenLoading = _isMovieTrendScreenLoading

    private val _isMovieTrendLoading = MutableStateFlow(false)
    val isMovieTrendLoading = _isMovieTrendLoading

    private val _trendMovieList = MutableStateFlow<List<Movie>>(listOf())
    val trendMovieList = _trendMovieList

    private val _trendMovieChipList = MutableStateFlow(trendingChipList)
    val trendMovieChipList = _trendMovieChipList

    private val _selectedMovieChipIndex = MutableStateFlow(DEFAULT_SELECTED_CHIP)
    val selectedMovieChipIndex = _selectedMovieChipIndex

    private val _selectedTvChipIndex = MutableStateFlow(DEFAULT_SELECTED_CHIP)
    val selectedTvChipIndex = _selectedTvChipIndex


    private val _trendTvList = MutableStateFlow<List<Movie>>(listOf())
    val trendTvList = _trendTvList

    private val _trendTvChipList = MutableStateFlow(trendingChipList)
    val trendTvChipList = _trendTvChipList

    private val _isTvTrendScreenLoading = MutableStateFlow(false)
    val isTvTrendScreenLoading = _isTvTrendScreenLoading

    private val _isTvTrendLoading = MutableStateFlow(false)
    val isTvTrendLoading = _isTvTrendLoading

    private val _selectedPeopleChipIndex = MutableStateFlow(DEFAULT_SELECTED_CHIP)
    val selectedPeopleChipIndex = _selectedPeopleChipIndex

    private val _trendPeopleList = MutableStateFlow<List<Movie>>(listOf())
    val trendPeopleList = _trendPeopleList

    private val _trendPeopleChipList = MutableStateFlow(trendingChipList)
    val trendPeopleChipList = _trendPeopleChipList

    private val _isPeopleTrendScreenLoading = MutableStateFlow(false)
    val isPeopleTrendScreenLoading = _isPeopleTrendScreenLoading

    private val _isPeopleTrendLoading = MutableStateFlow(false)
    val isPeopleTrendLoading = _isPeopleTrendLoading

    init {
        _isScreenLoading.value = true
        loadTrendingMedia(
            getSelectedTimeWindow(DEFAULT_SELECTED_CHIP),
            MEDIA_TYPE_MOVIE,
            true,
        )
        loadTrendingMedia(
            getSelectedTimeWindow(DEFAULT_SELECTED_CHIP),
            MEDIA_TYPE_TV,
            true,
        )

        loadTrendingMedia(
            getSelectedTimeWindow(DEFAULT_SELECTED_CHIP),
            MEDIA_TYPE_PEOPLE,
            true,
        )
    }

    private fun loadTrendingMedia(
        timeWindow: String,
        mediaType: String,
        isFirstLoad: Boolean,
    ) {
        try {
            viewModelScope.launch(Dispatchers.Main) {
                if (isFirstLoad) {
                    setScreenLoadingStateByMediaType(mediaType = mediaType, isLoading = true)
                } else {
                    setLoadingStateByMediaType(mediaType = mediaType, isLoading = true)
                }
                val movies =
                    trendingRepository.getTrending(
                        timeWindow,
                        mediaType,
                    ).list
                _isScreenLoading.value = false
                if (isFirstLoad) {
                    setScreenLoadingStateByMediaType(mediaType = mediaType, isLoading = false)
                } else {
                    setLoadingStateByMediaType(mediaType = mediaType, isLoading = false)
                }
                movies?.let {
                    when (mediaType) {
                        MEDIA_TYPE_MOVIE -> {
                            _trendMovieList.value = movies
                        }

                        MEDIA_TYPE_TV -> {
                            _trendTvList.value = movies
                        }

                        MEDIA_TYPE_PEOPLE -> {
                            _trendPeopleList.value = movies
                        }

                        else -> {
                        }
                    }
                } ?: run {
                    // TODO: handle fail case
                }
            }
        } catch (e: Exception) {
            Napier.d { "title" + e.message }
        }
    }

    private fun setScreenLoadingStateByMediaType(
        mediaType: String,
        isLoading: Boolean,
    ) {
        when (mediaType) {
            MEDIA_TYPE_MOVIE -> {
                _isMovieTrendScreenLoading.value = isLoading
            }

            MEDIA_TYPE_TV -> {
                _isTvTrendScreenLoading.value = isLoading
            }

            MEDIA_TYPE_PEOPLE -> {
                _isPeopleTrendScreenLoading.value = isLoading
            }

            else -> {
            }
        }
    }

    private fun setLoadingStateByMediaType(
        mediaType: String,
        isLoading: Boolean,
    ) {
        when (mediaType) {
            MEDIA_TYPE_MOVIE -> {
                _isMovieTrendLoading.value = isLoading
            }

            MEDIA_TYPE_TV -> {
                _isTvTrendLoading.value = isLoading
            }

            MEDIA_TYPE_PEOPLE -> {
                _isPeopleTrendLoading.value = isLoading
            }

            else -> {
            }
        }
    }

    fun onChipSelected(
        selectedIndex: Int,
        mediaType: String,
    ) {
        when (mediaType) {
            MEDIA_TYPE_MOVIE -> {
                _selectedMovieChipIndex.value = selectedIndex
            }

            MEDIA_TYPE_TV -> {
                _selectedTvChipIndex.value = selectedIndex
            }

            MEDIA_TYPE_PEOPLE -> {
                _selectedPeopleChipIndex.value = selectedIndex
            }

            else -> {
            }
        }

        loadTrendingMedia(
            getSelectedTimeWindow(selectedIndex),
            mediaType,
            false,
        )
    }

    private fun getSelectedTimeWindow(selectedIndex: Int): String {
        return when (selectedIndex) {
            0 -> {
                TIME_WINDOW_DAY
            }

            1 -> {
                TIME_WINDOW_WEEK
            }

            else -> {
                TIME_WINDOW_DAY
            }
        }
    }

    companion object {
        const val DEFAULT_SELECTED_CHIP = 0
    }
}