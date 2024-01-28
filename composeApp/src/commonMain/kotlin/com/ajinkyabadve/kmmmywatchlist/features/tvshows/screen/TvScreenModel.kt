package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen

import cafe.adriel.voyager.core.model.ScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TvScreenModel(tvFetchType: String = TOP_RATED_API_PATH) : ScreenModel {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    private val tvRepository = TvRepositoryImpl()
    internal val tvState = MutableStateFlow<TvListScreenState>(TvListScreenState.Loading)
    internal val tvFilterState = MutableStateFlow<TvFilterState>(
        TvFilterState.Success(
            selectedChip = 0,
            chipItemList = chipList
        )
    )

    init {
        loadTvShows(tvFetchType)
    }

    private fun loadTvShows(tvFetchTypeByChipTitle: String) {
        viewModelScope.launch {
            tvState.emit(TvListScreenState.Loading)
            try {
                val response = tvRepository.getTvShows(1, tvFetchTypeByChipTitle)
                response.list?.let {
                    tvState.emit(
                        TvListScreenState.Success(
                            it
                        )
                    )
                }
            } catch (e: Exception) {// TODO find another solution
                e.printStackTrace()
                tvState.emit(TvListScreenState.Error(e.message.toString()))

            }
        }
    }

    fun onChipSelected(selectedChipIndex: Int) {
        tvFilterState.update {
            TvFilterState.Success(selectedChipIndex, chipList)
        }
        loadTvShows(getTvFetchTypeByChipTitle(selectedChipIndex))
    }

    private companion object {
        const val TOP_RATED = "Top Rated"
        const val POPULAR = "Popular"
        const val ON_THE_AIR = "On The Air"
        const val AIRING_TODAY = "Airing Today"

        const val TOP_RATED_API_PATH = "top_rated"
        const val POPULAR_API_PATH = "popular"
        const val ON_THE_AIR_API_PATH = "on_the_air"
        const val AIRING_TODAY_API_PATH = "airing_today"
        val chipList = listOf(
            AIRING_TODAY,
            ON_THE_AIR,
            POPULAR,
            TOP_RATED
        )

        fun getChipTitleByIndex(index: Int): String {
            return chipList[index]
        }

        fun getTvFetchTypeByChipTitle(index: Int): String {
            return when (getChipTitleByIndex(index)) {
                AIRING_TODAY -> AIRING_TODAY_API_PATH
                TOP_RATED -> TOP_RATED_API_PATH
                ON_THE_AIR -> ON_THE_AIR_API_PATH
                POPULAR -> POPULAR_API_PATH
                else -> {
                    TOP_RATED_API_PATH
                }
            }

        }
    }
}


internal sealed interface TvFilterState {
    data class Success(
        val selectedChip: Int = 0,
        val chipItemList: List<String> = listOf()
    ) : TvFilterState
}

internal sealed interface TvListScreenState {
    data object Loading : TvListScreenState
    data class Error(val message: String) : TvListScreenState
    data class Success(
        val movieList: List<Tv> = listOf(),
    ) : TvListScreenState
}
