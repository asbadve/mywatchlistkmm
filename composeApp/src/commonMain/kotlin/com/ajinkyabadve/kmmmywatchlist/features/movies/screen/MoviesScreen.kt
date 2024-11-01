@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabDisposable
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.ajinkyabadve.kmmmywatchlist.design.movie.movieListScrollableChips
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant.NOW_PLAYING_MOVIES
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant.POPULAR_MOVIES
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant.TOP_RATED_MOVIES
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant.UPCOMING_MOVIES
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant.chipList
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.nowplaying.NowPlayingMovieScreen
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.popular.PopularMovieScreen
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.toprated.TopRatedMovieScreen
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.upcoming.UpcomingMovieScreen
import com.ajinkyabadve.kmmmywatchlist.homepage.model.AppTabs

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MoviesScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel =
            rememberScreenModel(AppTabs.MOVIES, factory = {
                MoviesScreenModel()
            })
        val movieFilterState = viewModel.movieFilterState.collectAsState()
        TabNavigator(NowPlayingMovieScreen, tabDisposable = {
            TabDisposable(
                navigator = it,
                tabs =
                    listOf(
                        NowPlayingMovieScreen,
                        UpcomingMovieScreen,
                        PopularMovieScreen,
                        TopRatedMovieScreen,
                    ),
            )
        }) { tabNavigator ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val stateFilter = movieFilterState.value as MovieFilterState.Success
                movieFilterChips(
                    stateFilter.selectedChip,
                    stateFilter.chipItemList,
                ) { selectedIndex ->
                    viewModel.onChipSelected(selectedIndex)
                    when (chipList[selectedIndex]) {
                        NOW_PLAYING_MOVIES -> {
                            tabNavigator.current = NowPlayingMovieScreen
                        }

                        UPCOMING_MOVIES -> {
                            tabNavigator.current = UpcomingMovieScreen
                        }

                        POPULAR_MOVIES -> {
                            tabNavigator.current = PopularMovieScreen
                        }

                        TOP_RATED_MOVIES -> {
                            tabNavigator.current = TopRatedMovieScreen
                        }
                    }
                }
                CurrentTab()
            }
        }
    }

    @Composable
    private fun movieFilterChips(
        selectedChip: Int,
        chipItemList: List<String>,
        onClick: (index: Int) -> Unit,
    ) {
        Column {
            movieListScrollableChips(
                selectedChip = selectedChip,
                chipItemList = chipItemList,
                onClick = { index ->
                    onClick.invoke(index)
                },
            )
        }
    }
}
