package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category

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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.core.getGridColumn
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant.NINTH_INDEX
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant.SIXTH_INDEX
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.mediaMovieRow
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun screenContent(
    viewModel: MovieListScreenModel,
    windowSizeClass: WindowSizeClass,
) {
    val movies = viewModel.movieList
    val lazyColumnListState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val shouldStartPaginate =
        remember {
            derivedStateOf {
                viewModel.canPaginate && (
                    lazyColumnListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        ?: -NINTH_INDEX
                ) >= (lazyColumnListState.layoutInfo.totalItemsCount - SIXTH_INDEX)
            }
        }
    LaunchedEffect(key1 = shouldStartPaginate.value) {
        if (shouldStartPaginate.value && viewModel.listState == ListState.IDLE) {
            viewModel.loadMovies()
        }
    }
    LazyVerticalGrid(
        state = lazyColumnListState,
        columns = GridCells.Fixed(windowSizeClass.getGridColumn()),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(movies) {
            mediaMovieRow(
                MoviesTab.Tabs.IMAGE_BASE_URL + it.posterPath,
                it.title,
            )
        }
        listStates(
            coroutineScope = coroutineScope,
            lazyColumnListState = lazyColumnListState,
            listState = viewModel.listState,
            networkRetryOnClick = {
                viewModel.loadMovies()
            },
        )
    }
}

fun LazyGridScope.listStates(
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

        CircularProgressIndicator(color = Color.Black)
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

        CircularProgressIndicator(color = Color.Black)
    }
}
