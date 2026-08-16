package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.listStates
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import com.ajinkyabadve.kmmmywatchlist.features.search.screen.searchMediaRow

private object AccountMediaGridConstant {
    const val PAGINATION_LOOKAHEAD_ITEMS = 3
    const val POSTER_TARGET_WIDTH_DP = 150
    val GRID_MIN_CELL_SIZE = 150.dp
}

/**
 * A paginated Favorites/Watchlist grid - mirrors `category.screenContent`'s layout and pagination
 * trigger exactly, but reuses [SearchResultItem]/[searchMediaRow] (Search's heterogeneous
 * movie-or-TV shape) instead of `Movie`, and reuses `category.listStates` for the
 * loading/error/exhausted footer unchanged - it already takes only [ListState], no
 * `MovieListScreenModel` coupling.
 */
@Composable
fun accountMediaGridContent(
    items: List<SearchResultItem>,
    listState: ListState,
    mediaType: SearchMediaType,
    emptyMessage: String,
    onLoadMore: () -> Unit,
    onItemSelected: (id: Long) -> Unit,
    lazyGridState: LazyGridState = rememberLazyGridState(),
) {
    val coroutineScope = rememberCoroutineScope()
    val shouldStartPaginate =
        remember {
            derivedStateOf {
                (
                    lazyGridState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: -AccountMediaGridConstant.PAGINATION_LOOKAHEAD_ITEMS
                ) >=
                    (lazyGridState.layoutInfo.totalItemsCount - AccountMediaGridConstant.PAGINATION_LOOKAHEAD_ITEMS)
            }
        }
    LaunchedEffect(shouldStartPaginate.value, listState) {
        if (shouldStartPaginate.value && listState == ListState.IDLE) {
            onLoadMore()
        }
    }

    if (items.isEmpty() && listState == ListState.PAGINATION_EXHAUST) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = emptyMessage,
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
        return
    }

    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Adaptive(minSize = AccountMediaGridConstant.GRID_MIN_CELL_SIZE),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(items, key = { it.uniqueKey }) { item ->
            val density = LocalDensity.current.density
            val imageUrl =
                ImageConfigResolver.resolve(
                    path = item.imagePath,
                    type = ImageConfigResolver.ImageType.POSTER,
                    targetWidthDp = AccountMediaGridConstant.POSTER_TARGET_WIDTH_DP,
                    density = density,
                )
            searchMediaRow(
                imageUrl = imageUrl,
                title = item.displayTitle,
                mediaType = mediaType,
                modifier = Modifier,
                onClick = { onItemSelected(item.id.toLong()) },
            )
        }
        listStates(
            coroutineScope = coroutineScope,
            lazyColumnListState = lazyGridState,
            listState = listState,
            networkRetryOnClick = onLoadMore,
        )
    }
}
