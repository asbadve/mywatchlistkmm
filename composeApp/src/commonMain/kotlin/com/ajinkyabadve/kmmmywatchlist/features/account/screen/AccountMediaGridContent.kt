package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchResultItem
import com.ajinkyabadve.kmmmywatchlist.features.search.screen.UpcomingBadge
import com.ajinkyabadve.kmmmywatchlist.features.search.screen.searchMediaRow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_retry
import org.jetbrains.compose.resources.stringResource

private object AccountMediaGridConstant {
    const val POSTER_TARGET_WIDTH_DP = 150
    val GRID_MIN_CELL_SIZE = 150.dp
    val BADGE_OFFSET = 12.dp
}

/**
 * A Paging3-backed Favorites/Watchlist grid - reuses [SearchResultItem]/[searchMediaRow] (Search's
 * heterogeneous movie-or-TV shape) instead of `Movie`. Pagination itself (fetching the next page as
 * the user scrolls) is driven by [lazyPagingItems] internally - see
 * `TrackedMediaRepository.pagedFlow`/`TrackedMediaRemoteMediator` - this composable only reacts to
 * [LazyPagingItems.loadState], it never triggers a fetch itself.
 */
@Composable
fun accountMediaGridContent(
    lazyPagingItems: LazyPagingItems<SearchResultItem>,
    mediaType: SearchMediaType,
    emptyMessage: String,
    onItemSelected: (id: Long) -> Unit,
    lazyGridState: LazyGridState = rememberLazyGridState(),
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    if (lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.refresh is LoadState.NotLoading) {
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
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it.uniqueKey },
        ) { index ->
            val item = lazyPagingItems[index] ?: return@items
            val density = LocalDensity.current.density
            val imageUrl =
                ImageConfigResolver.resolve(
                    path = item.imagePath,
                    type = ImageConfigResolver.ImageType.POSTER,
                    targetWidthDp = AccountMediaGridConstant.POSTER_TARGET_WIDTH_DP,
                    density = density,
                )
            Box {
                searchMediaRow(
                    imageUrl = imageUrl,
                    title = item.displayTitle,
                    mediaType = mediaType,
                    modifier = Modifier,
                    onClick = { onItemSelected(item.id.toLong()) },
                )
                if (item.isUpcoming(today)) {
                    UpcomingBadge(
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(AccountMediaGridConstant.BADGE_OFFSET),
                    )
                }
            }
        }
        when (val appendState = lazyPagingItems.loadState.append) {
            is LoadState.Loading ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

            is LoadState.Error ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(appendState.error.message.orEmpty(), textAlign = TextAlign.Center)
                        Button(onClick = { lazyPagingItems.retry() }) { Text(stringResource(Res.string.action_retry)) }
                    }
                }

            is LoadState.NotLoading -> Unit
        }
    }
}
