package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.mediaMovieRow
import com.ajinkyabadve.kmmmywatchlist.features.search.screen.UpcomingBadge
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private object DiscoverMovieTabConstant {
    const val PAGINATION_LOOKAHEAD_ITEMS = 3
    const val POSTER_TARGET_WIDTH_DP = 150
    val GRID_MIN_CELL_SIZE = 150.dp
    val BADGE_OFFSET = 12.dp
}

/**
 * The Movies screen's "Discover" sub-tab body - just the results grid. The filter trigger (an
 * [androidx.compose.material3.ExtendedFloatingActionButton] shown only while this tab is selected)
 * and [com.ajinkyabadve.kmmmywatchlist.features.discover.screen.DiscoverFilterDialog] it opens are
 * owned by [com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MovieScreenTabs] instead of here,
 * since the FAB has to float over the whole tab row, not just this sub-tab's content. Mirrors
 * [MovieListTab]'s lazy `viewModel(key = ...)` construction seam so the ScreenModel (and its network
 * call) only exists once this tab is actually selected.
 */
@Composable
fun DiscoverMovieTab(
    lazyGridState: LazyGridState = rememberLazyGridState(),
    onMovieSelected: (movieId: Long) -> Unit = {},
    screenModel: DiscoverMovieScreenModel? = null,
) {
    val viewModel = screenModel ?: viewModel(key = "DiscoverMovieScreenModel") { DiscoverMovieScreenModel() }
    discoverMovieGrid(viewModel, lazyGridState, onMovieSelected)
}

@Composable
private fun discoverMovieGrid(
    viewModel: DiscoverMovieScreenModel,
    lazyGridState: LazyGridState,
    onMovieSelected: (movieId: Long) -> Unit,
) {
    val movies = viewModel.movieList
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val shouldStartPaginate =
        remember {
            derivedStateOf {
                (
                    lazyGridState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: -DiscoverMovieTabConstant.PAGINATION_LOOKAHEAD_ITEMS
                ) >= (lazyGridState.layoutInfo.totalItemsCount - DiscoverMovieTabConstant.PAGINATION_LOOKAHEAD_ITEMS)
            }
        }
    LaunchedEffect(shouldStartPaginate.value, viewModel.listState) {
        if (shouldStartPaginate.value && viewModel.listState == ListState.IDLE) {
            viewModel.loadMovies()
        }
    }
    val coroutineScope = rememberCoroutineScope()
    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Adaptive(minSize = DiscoverMovieTabConstant.GRID_MIN_CELL_SIZE),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(movies, key = { it.id }) { movie ->
            val density = LocalDensity.current.density
            val imageUrl =
                ImageConfigResolver.resolve(
                    path = movie.posterPath,
                    type = ImageConfigResolver.ImageType.POSTER,
                    targetWidthDp = DiscoverMovieTabConstant.POSTER_TARGET_WIDTH_DP,
                    density = density,
                )
            Box {
                mediaMovieRow(imageUrl, movie.title, modifier = Modifier, onClick = { onMovieSelected(movie.id.toLong()) })
                if (movie.isUpcoming(today)) {
                    UpcomingBadge(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(DiscoverMovieTabConstant.BADGE_OFFSET),
                    )
                }
            }
        }
        listStates(
            coroutineScope = coroutineScope,
            lazyColumnListState = lazyGridState,
            listState = viewModel.listState,
            networkRetryOnClick = { viewModel.loadMovies() },
        )
    }
}
