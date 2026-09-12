package com.ajinkyabadve.kmmmywatchlist.features.account.screen

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
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FollowedCollection
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.mediaPersonRow
import kotlinx.coroutines.flow.Flow
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.collection_favorites_empty_message
import mywatchlist.composeapp.generated.resources.collection_favorites_empty_title
import mywatchlist.composeapp.generated.resources.collection_favorites_local_only_caveat
import org.jetbrains.compose.resources.stringResource

private object FavoriteCollectionsTabConstant {
    const val GRID_CELL_MIN_SIZE_DP = 150
    const val POSTER_TARGET_WIDTH_DP = 150
    val EMPTY_STATE_ICON_SIZE = 48.dp
}

/**
 * The "Collections" tab under My Fav (requested alongside Favorites/Watchlist/Lists) - lists every
 * locally-followed collection via [FavoriteCollectionRepository.observeFavoriteCollections],
 * reusing [mediaPersonRow] (poster + name + click - not actually person-specific despite its
 * package) so the grid looks and behaves identically to [PersonFavoritesTab][com.ajinkyabadve.kmmmywatchlist.features.person.screen.category.PersonFavoritesTab]'s
 * equivalent for people. Reads local SQLite directly, no network call, so there's no
 * loading/error state to render - the list is simply empty until something is followed.
 */
class FavoriteCollectionsScreenModel(
    private val favoriteCollectionRepository: FavoriteCollectionRepository = FavoriteCollectionRepositoryImpl(),
) : ViewModel() {
    val favoriteCollections: Flow<List<FollowedCollection>> = favoriteCollectionRepository.observeFavoriteCollections()
}

@Composable
fun FavoriteCollectionsTab(
    modifier: Modifier = Modifier,
    viewModel: FavoriteCollectionsScreenModel = viewModel { FavoriteCollectionsScreenModel() },
    lazyGridState: LazyGridState = rememberLazyGridState(),
    onCollectionSelected: (collectionId: Long) -> Unit = {},
) {
    val favoriteCollections by viewModel.favoriteCollections.collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize()) {
        // Always visible, not just once something's followed - see FollowCollectionButton's
        // identical caveat for why this needs to be said explicitly: TMDB has no favorite/follow
        // API for collections, so unlike this same screen's movie/TV counterparts, nothing here
        // ever syncs.
        Text(
            text = stringResource(Res.string.collection_favorites_local_only_caveat),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (favoriteCollections.isEmpty()) {
            FavoriteCollectionsEmptyState()
        } else {
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Adaptive(minSize = FavoriteCollectionsTabConstant.GRID_CELL_MIN_SIZE_DP.dp),
                contentPadding = PaddingValues(8.dp),
            ) {
                items(favoriteCollections) { collection ->
                    val density = LocalDensity.current.density
                    val imageUrl =
                        ImageConfigResolver.resolve(
                            path = collection.posterPath,
                            type = ImageConfigResolver.ImageType.POSTER,
                            targetWidthDp = FavoriteCollectionsTabConstant.POSTER_TARGET_WIDTH_DP,
                            density = density,
                        )
                    mediaPersonRow(
                        imageUrl = imageUrl,
                        name = collection.name,
                        onClick = { onCollectionSelected(collection.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCollectionsEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(FavoriteCollectionsTabConstant.EMPTY_STATE_ICON_SIZE).padding(bottom = 12.dp),
        )
        Text(
            text = stringResource(Res.string.collection_favorites_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(Res.string.collection_favorites_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
