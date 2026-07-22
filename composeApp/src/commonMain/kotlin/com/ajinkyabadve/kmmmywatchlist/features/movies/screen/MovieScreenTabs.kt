package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.MovieListScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.screenContent

sealed class MovieTab(
    val title: String,
) {
    data object NowPlaying : MovieTab("Now Playing")

    data object Upcoming : MovieTab("Upcoming")

    data object Popular : MovieTab("Popular")

    data object TopRated : MovieTab("Top Rated")
}

/**
 * A composable that displays movie tabs and the content for the selected tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieScreenTabs(
    modifier: Modifier = Modifier,
    nowPlayingViewModel: MovieListScreenModel,
    upcomingViewModel: MovieListScreenModel,
    popularViewModel: MovieListScreenModel,
    topRatedViewModel: MovieListScreenModel,
    onMovieSelected: (movieId: Long) -> Unit,
) {
    val tabs =
        remember {
            listOf(
                MovieTab.NowPlaying,
                MovieTab.Upcoming,
                MovieTab.Popular,
                MovieTab.TopRated,
            )
        }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    val nowPlayingGridState = rememberLazyGridState()
    val upcomingGridState = rememberLazyGridState()
    val popularGridState = rememberLazyGridState()
    val topRatedGridState = rememberLazyGridState()

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val tabsWidth = 400.dp
            val calculatedPadding = (maxWidth - tabsWidth) / 2
            val edgePadding = if (calculatedPadding > 0.dp) calculatedPadding else 0.dp

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = edgePadding,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                indicator = {
                    Box(
                        modifier =
                            Modifier
                                .tabIndicatorOffset(selectedTabIndex)
                                .padding(horizontal = 24.dp)
                                .height(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                ),
                    )
                },
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab.title) },
                    )
                }
            }
        }
        when (tabs[selectedTabIndex]) {
            MovieTab.NowPlaying -> MovieListTab(nowPlayingViewModel, nowPlayingGridState, onMovieSelected)
            MovieTab.Upcoming -> MovieListTab(upcomingViewModel, upcomingGridState, onMovieSelected)
            MovieTab.Popular -> MovieListTab(popularViewModel, popularGridState, onMovieSelected)
            MovieTab.TopRated -> MovieListTab(topRatedViewModel, topRatedGridState, onMovieSelected)
        }
    }
}

@Composable
fun MovieListTab(
    viewModel: MovieListScreenModel,
    lazyGridState: LazyGridState,
    onMovieSelected: (movieId: Long) -> Unit,
) {
    screenContent(viewModel = viewModel, lazyColumnListState = lazyGridState, onMovieSelected = onMovieSelected)
}
