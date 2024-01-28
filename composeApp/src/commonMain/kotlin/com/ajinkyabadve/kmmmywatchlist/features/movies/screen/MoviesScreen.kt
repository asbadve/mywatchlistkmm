@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

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
import com.ajinkyabadve.kmmmywatchlist.design.movie.MovieCard
import com.ajinkyabadve.kmmmywatchlist.design.movie.movieListScrollableChips
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import com.seiko.imageloader.rememberImagePainter

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MoviesScreen : Screen {
    @Composable
    override fun Content() {
        val windowSizeClass = calculateWindowSizeClass()
        val viewModel = rememberScreenModel(MoviesTab.Tabs.MOVIES, factory = {
            MoviesScreenModel()
        })
        val state = viewModel.movieState.collectAsState()
        val movieFilterState = viewModel.movieFilterState.collectAsState()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            movieFilterChips(movieFilterState, viewModel)
            when (val result = state.value) {
                is MovieListScreenState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is MovieListScreenState.Success -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LazyVerticalGrid(
                            state = rememberLazyGridState(),
                            columns = GridCells.Fixed(windowSizeClass.getGridColumn()),
                            contentPadding = PaddingValues(8.dp),
                        ) {
                            items(result.movieList) { movie ->
                                movieRow(
                                    MoviesTab.Tabs.IMAGE_BASE_URL + movie.posterPath,
                                    movie.title,
                                )
                            }
                        }
                    }
                }

                is MovieListScreenState.Error -> {
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
    private fun movieFilterChips(
        movieFilterState: State<MovieFilterState>, viewModel: MoviesScreenModel
    ) {
        movieFilterState.value.let {
            if (it is MovieFilterState.Success) {
                movieListScrollableChips(selectedChip = it.selectedChip,
                    chipItemList = it.chipItemList,
                    onClick = { index ->
                        viewModel.onSelectChip(index)
                    })
            }
        }
    }

    @Composable
    private fun movieRow(imageUrl: String?, title: String) {
        var painter: Painter? = null
        imageUrl?.let {
            painter = rememberImagePainter(url = imageUrl, filterQuality = FilterQuality.Medium)
        }
        Row(Modifier.padding(8.dp)) {
            painter?.let {
                MovieCard(
                    Modifier,
                    title,
                    it,
                )
            }
        }
    }
}

