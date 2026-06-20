package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowsConstant
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowsConstant.SIXTH_INDEX
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowsConstant.THIRD_INDEX
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.mediaTvShowRow
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun tvShowScreenContent(
    viewModel: TvListScreenModel,
    lazyColumnListState: LazyGridState = rememberLazyGridState(),
    onTvShowSelected: ((tvShowId: Long) -> Unit)? = null,
) {
    val tvShows = viewModel.tvList
    val coroutineScope = rememberCoroutineScope()
    val shouldStartPaginate =
        remember {
            derivedStateOf {
                (
                    lazyColumnListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        ?: -SIXTH_INDEX
                ) >= (lazyColumnListState.layoutInfo.totalItemsCount - THIRD_INDEX)
            }
        }
    LaunchedEffect(key1 = shouldStartPaginate.value, key2 = viewModel.listState) {
        if (shouldStartPaginate.value && viewModel.listState == ListState.IDLE) {
            viewModel.loadTvShows()
        }
    }
    LazyVerticalGrid(
        state = lazyColumnListState,
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(tvShows) {
            mediaTvShowRow(
                TvShowsConstant.IMAGE_BASE_URL + it.posterPath,
                it.title,
                modifier = Modifier,
                onClick = {
                    Napier.d { "title" + it.title }
                    onTvShowSelected?.invoke(it.id.toLong())
                },
            )
        }
        tvListStates(
            coroutineScope = coroutineScope,
            lazyColumnListState = lazyColumnListState,
            listState = viewModel.listState,
            networkRetryOnClick = {
                viewModel.loadTvShows()
            },
        )
    }
}

fun LazyGridScope.tvListStates(
    coroutineScope: CoroutineScope,
    lazyColumnListState: LazyGridState,
    listState: ListState,
    networkRetryOnClick: () -> Unit,
) {
    item(
        key = listState,
    ) {
        when (listState) {
            ListState.LOADING -> {
                firstLoadingState()
            }

            ListState.PAGINATING -> {
                pageLoadingState()
            }

            ListState.PAGINATION_EXHAUST -> {
                paginationExhaustState(coroutineScope, lazyColumnListState)
            }

            ListState.NETWORK_ERROR -> {
                showNetworkErrorWithRetry(networkRetryOnClick)
            }

            else -> {
            }
        }
    }
}

@Composable
private fun showNetworkErrorWithRetry(onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TextButton(
            modifier =
                Modifier
                    .padding(top = 8.dp),
            onClick = onClick,
            content = {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "",
                    )
                    Text(text = "Retry")
                }
            },
        )
    }
}

@Composable
private fun paginationExhaustState(
    coroutineScope: CoroutineScope,
    lazyColumnListState: LazyGridState,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = Icons.Rounded.Face, contentDescription = "")

        Text(text = "Nothing left.")

        TextButton(
            modifier =
                Modifier
                    .padding(top = 8.dp),
            onClick = {
                coroutineScope.launch {
                    lazyColumnListState.animateScrollToItem(0)
                }
            },
            content = {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "",
                    )

                    Text(text = "Back to Top")

                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "",
                    )
                }
            },
        )
    }
}

@Composable
private fun pageLoadingState() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Pagination Loading")

        CircularProgressIndicator()
    }
}

@Composable
private fun firstLoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier =
                Modifier
                    .padding(8.dp),
            text = "Refresh Loading",
        )

        CircularProgressIndicator()
    }
}
