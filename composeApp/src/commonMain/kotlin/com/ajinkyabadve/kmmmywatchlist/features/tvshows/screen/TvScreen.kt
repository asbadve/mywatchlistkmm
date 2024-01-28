package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.ajinkyabadve.kmmmywatchlist.core.getGridColumn
import com.ajinkyabadve.kmmmywatchlist.design.movie.MediaCard
import com.ajinkyabadve.kmmmywatchlist.design.movie.movieListScrollableChips
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import com.seiko.imageloader.rememberImagePainter

class TvScreen : Screen {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    override fun Content() {
        val windowSizeClass = calculateWindowSizeClass()
        val viewModel = rememberScreenModel(MoviesTab.Tabs.TV_SHOWS, factory = {
            TvScreenModel()
        })
        val state = viewModel.tvState.collectAsState()
        val tvFilterStateState = viewModel.tvFilterState.collectAsState()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            tvFilterChips(tvFilterStateState, viewModel)
            when (val result = state.value) {
                is TvListScreenState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is TvListScreenState.Success -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LazyVerticalGrid(
                            state = rememberLazyGridState(),
                            columns = GridCells.Fixed(windowSizeClass.getGridColumn()),
                            contentPadding = PaddingValues(8.dp),
                        ) {
                            items(result.movieList) { movie ->
                                tvRow(
                                    MoviesTab.Tabs.IMAGE_BASE_URL + movie.posterPath,
                                    movie.title,
                                )
                            }
                        }
                    }
                }

                is TvListScreenState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Text(text = result.message)
                    }
                }
            }
        }

    }

    @Composable
    private fun tvFilterChips(
        tvFilterStateState: State<TvFilterState>, viewModel: TvScreenModel
    ) {
        tvFilterStateState.value.let {
            if (it is TvFilterState.Success) {
                movieListScrollableChips(selectedChip = it.selectedChip,
                    chipItemList = it.chipItemList,
                    onClick = { index ->
                        viewModel.onChipSelected(index)
                    })
            }
        }
    }

    @Composable
    private fun tvRow(imageUrl: String?, title: String) {
        var painter: Painter? = null
        imageUrl?.let {
            painter = rememberImagePainter(url = imageUrl, filterQuality = FilterQuality.Medium)
        }
        Row(Modifier.padding(8.dp)) {
            painter?.let {
                MediaCard(
                    Modifier,
                    title,
                    it,
                )
            }
        }
    }
}