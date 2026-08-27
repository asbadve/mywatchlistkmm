package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

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
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.category.PersonFavoritesTab
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.category.PersonListScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.category.personListScreenContent
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.filter_popular
import mywatchlist.composeapp.generated.resources.tab_favorites
import org.jetbrains.compose.resources.stringResource

private sealed interface PersonTab {
    data object Popular : PersonTab

    data object Favorites : PersonTab
}

/**
 * The top-level "People" destination - "Popular" (TMDB's `/person/popular`,
 * [personListScreenContent]) alongside "Favorites" (requested 2026-08-26: local-only followed
 * people, [PersonFavoritesTab]), behind the same [PillTabRow] chrome [MovieScreenTabs]/[MyFavTabs]
 * use elsewhere - unlike those, "Favorites" here needs no signed-in session at all, since following
 * a person is never tied to the TMDB account (see `FavoritePersonRepository`'s kdoc).
 */
@Composable
fun PersonScreenTab(
    modifier: Modifier = Modifier,
    viewModel: PersonListScreenModel = viewModel { PersonListScreenModel() },
    onPersonSelected: (personId: Long) -> Unit = {},
) {
    val tabs = remember { listOf(PersonTab.Popular, PersonTab.Favorites) }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val tabTitles = listOf(stringResource(Res.string.filter_popular), stringResource(Res.string.tab_favorites))

    Column(modifier = modifier.fillMaxSize()) {
        PillTabRow(
            tabs = tabTitles,
            selectedIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
        )
        when (tabs[selectedTabIndex]) {
            PersonTab.Popular ->
                personListScreenContent(
                    viewModel = viewModel,
                    onPersonSelected = onPersonSelected,
                )
            PersonTab.Favorites ->
                PersonFavoritesTab(onPersonSelected = onPersonSelected)
        }
    }
}
