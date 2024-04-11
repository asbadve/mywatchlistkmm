package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MoviesScreenModel : ScreenModel {
    internal val movieFilterState = MutableStateFlow<MovieFilterState>(
        MovieFilterState.Success(
            selectedChip = 0,
            chipItemList = MoviesConstant.chipList
        )
    )

    override fun onDispose() {
        super.onDispose()
    }

    fun onChipSelected(selectedChipIndex: Int) {
        movieFilterState.update {
            MovieFilterState.Success(selectedChipIndex, MoviesConstant.chipList)
        }
    }

}


