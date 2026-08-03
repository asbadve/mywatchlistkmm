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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
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
    onMovieSelected: (movieId: Long) -> Unit,
    // Test-only seam: lets UI tests inject a fake repository so each sub-tab's ScreenModel can be
    // verified to load lazily (on first selection) and cache (no refetch on re-selection) without
    // hitting real network. This overrides only the *repository*, not the ScreenModel itself -
    // construction still happens inside MovieListTab's `viewModel(key = ...) { }` factory, which
    // Compose only invokes once that sub-tab's `when` branch is actually selected. Passing a
    // pre-built ScreenModel instead would defeat the point: Kotlin evaluates constructor-call
    // arguments eagerly, so all four would construct (and fire their init{} load) up front,
    // regardless of which tab is selected. Production call sites never pass these.
    nowPlayingRepository: MovieRepository? = null,
    upcomingRepository: MovieRepository? = null,
    popularRepository: MovieRepository? = null,
    topRatedRepository: MovieRepository? = null,
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
            MovieTab.NowPlaying ->
                MovieListTab(MoviesConstant.NOW_PLAYING_API_PATH, nowPlayingGridState, onMovieSelected, nowPlayingRepository)
            MovieTab.Upcoming ->
                MovieListTab(MoviesConstant.UPCOMING_API_PATH, upcomingGridState, onMovieSelected, upcomingRepository)
            MovieTab.Popular ->
                MovieListTab(MoviesConstant.POPULAR_API_PATH, popularGridState, onMovieSelected, popularRepository)
            MovieTab.TopRated ->
                MovieListTab(MoviesConstant.TOP_RATED_API_PATH, topRatedGridState, onMovieSelected, topRatedRepository)
        }
    }
}

@Composable
fun MovieListTab(
    fetchType: String,
    lazyGridState: LazyGridState,
    onMovieSelected: (movieId: Long) -> Unit,
    movieRepository: MovieRepository? = null,
) {
    val viewModel =
        viewModel(key = "MovieListScreenModel:$fetchType") {
            if (movieRepository != null) MovieListScreenModel(fetchType, movieRepository) else MovieListScreenModel(fetchType)
        }
    screenContent(viewModel = viewModel, lazyColumnListState = lazyGridState, onMovieSelected = onMovieSelected)
}
