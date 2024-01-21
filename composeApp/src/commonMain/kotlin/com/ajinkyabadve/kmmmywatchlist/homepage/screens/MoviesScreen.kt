@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.ajinkyabadve.kmmmywatchlist.homepage.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MoviesScreen(private val movieFetchType: String) : Screen {
    @Composable
    override fun Content() {
        val windowSizeClass = calculateWindowSizeClass()

        val viewModel = rememberScreenModel(
            movieFetchType, factory = {
                MoviesScreenModel(movieFetchType)
            })

        val state = viewModel.state.collectAsState()
        when (val result = state.value) {
            is MovieListScreenState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is MovieListScreenState.Success -> {
                LazyVerticalGrid(
                    state = rememberLazyGridState(),
                    columns = GridCells.Fixed(MoviesTab.getGridColumn(windowSizeClass)),
                    contentPadding = PaddingValues(8.dp),
                ) {
                    items(result.countriesList) { movie ->
                        MoviesTab.MovieRow(
                            MoviesTab.Tabs.IMAGE_BASE_URL + movie.posterPath,
                            movie.title,
                        )
                    }
                }
            }

            is MovieListScreenState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = result.message)
                }
            }

            else -> {}
        }


    }
}