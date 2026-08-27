package com.ajinkyabadve.kmmmywatchlist.features.person.screen.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.features.person.model.Person
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.mediaPersonRow
import kotlinx.coroutines.flow.Flow
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.person_favorites_empty_message
import mywatchlist.composeapp.generated.resources.person_favorites_empty_title
import mywatchlist.composeapp.generated.resources.person_favorites_local_only_caveat
import org.jetbrains.compose.resources.stringResource

private object PersonFavoritesTabConstant {
    const val GRID_CELL_MIN_SIZE_DP = 150
    const val PROFILE_TARGET_WIDTH_DP = 150
    val EMPTY_STATE_ICON_SIZE = 48.dp
}

/**
 * The Person tab's "Favorites" sub-tab (requested 2026-08-26, alongside "Popular" -
 * [personListScreenContent]) - lists every locally-followed person via
 * [FavoritePersonRepository.observeFavoritePeople], reusing [mediaPersonRow] so the grid looks and
 * behaves identically to the Popular sub-tab's. Reads local SQLite directly, no network call, so
 * there's no loading/error state to render - the list is simply empty until something is followed.
 */
class PersonFavoritesScreenModel(
    private val favoritePersonRepository: FavoritePersonRepository = FavoritePersonRepositoryImpl(),
) : ViewModel() {
    val favoritePeople: Flow<List<Person>> = favoritePersonRepository.observeFavoritePeople()
}

@Composable
fun PersonFavoritesTab(
    modifier: Modifier = Modifier,
    viewModel: PersonFavoritesScreenModel = viewModel { PersonFavoritesScreenModel() },
    lazyGridState: LazyGridState = rememberLazyGridState(),
    onPersonSelected: (personId: Long) -> Unit = {},
) {
    val favoritePeople by viewModel.favoritePeople.collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize()) {
        // Always visible, not just once someone's followed - see FollowPersonButton's identical
        // caveat for why this needs to be said explicitly: TMDB has no favorite/follow API for
        // people, so unlike this same screen's movie/TV counterparts, nothing here ever syncs.
        Text(
            text = stringResource(Res.string.person_favorites_local_only_caveat),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (favoritePeople.isEmpty()) {
            PersonFavoritesEmptyState()
        } else {
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Adaptive(minSize = PersonFavoritesTabConstant.GRID_CELL_MIN_SIZE_DP.dp),
                contentPadding = PaddingValues(8.dp),
            ) {
                items(favoritePeople) { person ->
                    val density = LocalDensity.current.density
                    val imageUrl =
                        ImageConfigResolver.resolve(
                            path = person.profilePath,
                            type = ImageConfigResolver.ImageType.PROFILE,
                            targetWidthDp = PersonFavoritesTabConstant.PROFILE_TARGET_WIDTH_DP,
                            density = density,
                        )
                    mediaPersonRow(
                        imageUrl = imageUrl,
                        name = person.name,
                        onClick = { onPersonSelected(person.id.toLong()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonFavoritesEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(PersonFavoritesTabConstant.EMPTY_STATE_ICON_SIZE).padding(bottom = 12.dp),
        )
        Text(
            text = stringResource(Res.string.person_favorites_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(Res.string.person_favorites_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
