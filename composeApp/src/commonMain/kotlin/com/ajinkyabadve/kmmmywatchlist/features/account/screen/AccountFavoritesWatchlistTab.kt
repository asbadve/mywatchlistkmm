package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.design.movie.scrollableChips
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.AccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.ListState
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.favorites_empty_message
import mywatchlist.composeapp.generated.resources.media_type_movie
import mywatchlist.composeapp.generated.resources.media_type_tv
import mywatchlist.composeapp.generated.resources.watchlist_empty_message
import org.jetbrains.compose.resources.stringResource

/** Not `private`: tests drive the pull-to-refresh gesture through this tag. */
object AccountFavoritesWatchlistTabConstant {
    const val PULL_TO_REFRESH_TAG = "AccountMediaPullToRefresh"
}

/**
 * Favorites and Watchlist share this exact layout - a Movie/TV chip toggle over a paginated grid -
 * only the endpoint category and empty-state copy differ, so both tabs in `MyFavTabs` render the
 * same composable rather than two near-identical ones.
 */
@Composable
fun AccountFavoritesWatchlistTab(
    category: AccountMediaCategory,
    session: UserSession,
    onMovieSelected: (movieId: Long) -> Unit,
    onTvSelected: (tvId: Long) -> Unit,
    modifier: Modifier = Modifier,
    // Test-only seam, same pattern as MovieScreenTabs' per-tab repository overrides: lets a UI
    // test inject a fake so this composable never hits the real network.
    accountMediaRepository: AccountMediaRepository? = null,
) {
    val mediaTypes = remember { listOf(SearchMediaType.MOVIE, SearchMediaType.TV) }
    var selectedChip by rememberSaveable { mutableStateOf(0) }
    val selectedMediaType = mediaTypes[selectedChip]
    val chipLabels = listOf(stringResource(Res.string.media_type_movie), stringResource(Res.string.media_type_tv))
    val emptyMessage =
        stringResource(
            if (category == AccountMediaCategory.FAVORITES) Res.string.favorites_empty_message else Res.string.watchlist_empty_message,
        )

    val screenModel =
        viewModel(key = "AccountMedia:$category:$selectedMediaType") {
            if (accountMediaRepository != null) {
                AccountMediaListScreenModel(
                    category = category,
                    mediaType = selectedMediaType,
                    accountId = session.accountId,
                    sessionId = session.sessionId,
                    accountMediaRepository = accountMediaRepository,
                )
            } else {
                AccountMediaListScreenModel(
                    category = category,
                    mediaType = selectedMediaType,
                    accountId = session.accountId,
                    sessionId = session.sessionId,
                )
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            scrollableChips(
                selectedChip = selectedChip,
                chipItemList = chipLabels,
                onClick = { selectedChip = it },
            )
        }
        PullToRefreshBox(
            modifier = Modifier.weight(1f).fillMaxSize().testTag(AccountFavoritesWatchlistTabConstant.PULL_TO_REFRESH_TAG),
            isRefreshing = screenModel.listState == ListState.LOADING,
            onRefresh = { screenModel.refresh() },
        ) {
            accountMediaGridContent(
                items = screenModel.items,
                listState = screenModel.listState,
                mediaType = selectedMediaType,
                emptyMessage = emptyMessage,
                onLoadMore = { screenModel.load() },
                onItemSelected = { id ->
                    if (selectedMediaType == SearchMediaType.MOVIE) onMovieSelected(id) else onTvSelected(id)
                },
            )
        }
    }
}
