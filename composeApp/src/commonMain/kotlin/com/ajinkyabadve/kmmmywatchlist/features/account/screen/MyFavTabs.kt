package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.ui.PillTabRow
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.AccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.tab_favorites
import mywatchlist.composeapp.generated.resources.tab_lists
import mywatchlist.composeapp.generated.resources.tab_watchlist
import org.jetbrains.compose.resources.stringResource

private sealed interface MyFavTab {
    data object Favorites : MyFavTab

    data object Watchlist : MyFavTab

    data object Lists : MyFavTab
}

/** The signed-in "My Fav" content: Favorites / Watchlist / Lists, behind the same [PillTabRow] chrome `MovieScreenTabs` uses. */
@Composable
fun MyFavTabs(
    session: UserSession,
    onMovieSelected: (movieId: Long) -> Unit,
    onTvSelected: (tvId: Long) -> Unit,
    onListSelected: (listId: Long) -> Unit,
    modifier: Modifier = Modifier,
    // Test-only seams, same pattern MovieScreenTabs uses for its per-tab repositories.
    accountMediaRepository: AccountMediaRepository? = null,
    listsRepository: ListsRepository? = null,
) {
    val tabs = remember { listOf(MyFavTab.Favorites, MyFavTab.Watchlist, MyFavTab.Lists) }
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val tabTitles =
        listOf(
            stringResource(Res.string.tab_favorites),
            stringResource(Res.string.tab_watchlist),
            stringResource(Res.string.tab_lists),
        )

    Column(modifier = modifier.fillMaxSize()) {
        PillTabRow(
            tabs = tabTitles,
            selectedIndex = selectedIndex,
            onTabSelected = { selectedIndex = it },
        )
        when (tabs[selectedIndex]) {
            MyFavTab.Favorites ->
                AccountFavoritesWatchlistTab(
                    category = AccountMediaCategory.FAVORITES,
                    session = session,
                    onMovieSelected = onMovieSelected,
                    onTvSelected = onTvSelected,
                    accountMediaRepository = accountMediaRepository,
                )

            MyFavTab.Watchlist ->
                AccountFavoritesWatchlistTab(
                    category = AccountMediaCategory.WATCHLIST,
                    session = session,
                    onMovieSelected = onMovieSelected,
                    onTvSelected = onTvSelected,
                    accountMediaRepository = accountMediaRepository,
                )

            MyFavTab.Lists ->
                ListsTab(
                    session = session,
                    onListSelected = onListSelected,
                    screenModel =
                        viewModel(key = "ListsScreenModel:${session.accountId}") {
                            ListsScreenModel(
                                accountId = session.accountId,
                                sessionId = session.sessionId,
                                listsRepository = listsRepository ?: ListsRepositoryImpl(),
                            )
                        },
                )
        }
    }
}
