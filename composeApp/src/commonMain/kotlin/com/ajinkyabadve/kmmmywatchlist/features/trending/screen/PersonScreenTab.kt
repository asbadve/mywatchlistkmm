package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.category.PersonListScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.category.personListScreenContent

@Composable
fun PersonScreenTab(
    modifier: Modifier = Modifier,
    viewModel: PersonListScreenModel = viewModel { PersonListScreenModel() },
    onPersonSelected: (personId: Long) -> Unit = {},
) {
    personListScreenContent(
        viewModel = viewModel,
        onPersonSelected = onPersonSelected,
    )
}
