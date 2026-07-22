package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.category.PersonListScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.category.personListScreenContent

@Composable
fun PersonScreenTab(
    modifier: Modifier = Modifier,
    viewModel: PersonListScreenModel,
    onPersonSelected: (personId: Long) -> Unit = {},
) {
    personListScreenContent(
        viewModel = viewModel,
        onPersonSelected = onPersonSelected,
    )
}
