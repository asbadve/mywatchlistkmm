package com.ajinkyabadve.kmmmywatchlist.features.account.viewmodel

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.FavoriteCollectionsTab
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.FollowedCollection
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepositoryImpl
import kotlinx.coroutines.flow.Flow

/**
 * Backs [FavoriteCollectionsTab], the "Collections" tab under My Fav (requested alongside Favorites/Watchlist/Lists) - lists every
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
