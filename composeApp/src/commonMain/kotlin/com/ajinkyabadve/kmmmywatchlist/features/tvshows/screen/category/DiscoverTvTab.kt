package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category

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
import com.ajinkyabadve.kmmmywatchlist.features.search.screen.UpcomingBadge
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.mediaTvShowRow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private object DiscoverTvTabConstant {
    const val PAGINATION_LOOKAHEAD_ITEMS = 3
    const val POSTER_TARGET_WIDTH_DP = 150
    val GRID_MIN_CELL_SIZE = 150.dp
    val BADGE_OFFSET = 12.dp
}

/**
 * TV mirror of [com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.DiscoverMovieTab] -
 * just the results grid, the FAB and filter dialog live in
 * [com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowScreenTabs].
 */
@Composable
fun DiscoverTvTab(
    lazyGridState: LazyGridState = rememberLazyGridState(),
    onTvShowSelected: (tvShowId: Long) -> Unit = {},
    screenModel: DiscoverTvScreenModel? = null,
) {
    val viewModel = screenModel ?: viewModel(key = "DiscoverTvScreenModel") { DiscoverTvScreenModel() }
    discoverTvGrid(viewModel, lazyGridState, onTvShowSelected)
}

@Composable
private fun discoverTvGrid(
    viewModel: DiscoverTvScreenModel,
    lazyGridState: LazyGridState,
    onTvShowSelected: (tvShowId: Long) -> Unit,
) {
    val tvShows = viewModel.tvList
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val shouldStartPaginate =
        remember {
            derivedStateOf {
                (
                    lazyGridState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: -DiscoverTvTabConstant.PAGINATION_LOOKAHEAD_ITEMS
                ) >= (lazyGridState.layoutInfo.totalItemsCount - DiscoverTvTabConstant.PAGINATION_LOOKAHEAD_ITEMS)
            }
        }
    LaunchedEffect(shouldStartPaginate.value, viewModel.listState) {
        if (shouldStartPaginate.value && viewModel.listState == ListState.IDLE) {
            viewModel.loadTvShows()
        }
    }
    val coroutineScope = rememberCoroutineScope()
    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Adaptive(minSize = DiscoverTvTabConstant.GRID_MIN_CELL_SIZE),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(tvShows, key = { it.id }) { tvShow ->
            val density = LocalDensity.current.density
            val imageUrl =
                ImageConfigResolver.resolve(
                    path = tvShow.posterPath,
                    type = ImageConfigResolver.ImageType.POSTER,
                    targetWidthDp = DiscoverTvTabConstant.POSTER_TARGET_WIDTH_DP,
                    density = density,
                )
            Box {
                mediaTvShowRow(imageUrl, tvShow.title, modifier = Modifier, onClick = { onTvShowSelected(tvShow.id.toLong()) })
                if (tvShow.isUpcoming(today)) {
                    UpcomingBadge(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(DiscoverTvTabConstant.BADGE_OFFSET),
                    )
                }
            }
        }
        tvListStates(
            coroutineScope = coroutineScope,
            lazyColumnListState = lazyGridState,
            listState = viewModel.listState,
            networkRetryOnClick = { viewModel.loadTvShows() },
        )
    }
}
