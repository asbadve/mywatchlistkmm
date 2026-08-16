package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.design.util.addShimmerLoadingAnimation
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.AddToListDialog
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthScreenModelDefaults
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthUiState
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_add_to_list
import mywatchlist.composeapp.generated.resources.favorite_content_description
import mywatchlist.composeapp.generated.resources.watchlist_content_description
import org.jetbrains.compose.resources.stringResource

private object MediaActionButtonsConstant {
    val BUTTON_SIZE = 44.dp
    val CORNER_RADIUS = 8.dp
}

/**
 * Favorite/Watchlist/Add-to-list icon buttons for a movie or TV hero - styled like the trailer icon
 * `HeroActionRow`/`TvActionRow` already draw.
 *
 * Pure presentational component: no repository, no `ViewModel`, no `uiState` read here - every
 * value it draws ([isFavorite], [isInWatchlist], [isLoading]) and every action it can trigger
 * ([onFavoriteClick]/[onWatchlistClick]/[onAddToListClick]) is a plain parameter. The caller (see
 * [MediaActionButtonsSection] below, called from each hero section) is the one that owns a
 * `ViewModel` instance, reads its `StateFlow`, and decides what each callback does - this
 * composable only renders what it's told and reports what was clicked, per code-conventions §7.
 *
 * [isLoading] swaps every icon for a shimmer placeholder (same size, [Modifier.addShimmerLoadingAnimation]
 * - the util `MovieCard` already uses for its loading state, not a new mechanism) until the
 * `account_states` pre-check resolves, rather than guessing "not favorited" for a heartbeat and
 * popping to filled if it actually was. That fetch runs on the screen's own `ViewModel` alongside,
 * not before, the rest of the detail load (see `MovieDetailScreenModel`'s `init` block), so this
 * only ever gates the icons themselves - the rest of the screen never waits on it.
 *
 * Watchlist uses [Icons.Filled.List]/[Icons.Filled.CheckCircle] rather than a bookmark glyph:
 * `material-icons-core` (the dependency this project uses, not `-extended`) ships a small, fixed
 * icon set with no `Bookmark`/`BookmarkBorder`/`PlaylistAdd` - confirmed by listing
 * `material-icons-core-1.7.3-sources.jar`'s `filled/` package rather than assuming from memory.
 */
@Composable
internal fun MediaActionButtons(
    isFavorite: Boolean,
    isInWatchlist: Boolean,
    showAddToList: Boolean,
    isLoading: Boolean,
    colors: HeroColors,
    onFavoriteClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onAddToListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isLoading) {
            MediaActionShimmerButton()
            MediaActionShimmerButton()
            if (showAddToList) MediaActionShimmerButton()
        } else {
            MediaActionIconButton(
                icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(Res.string.favorite_content_description),
                colors = colors,
                onClick = onFavoriteClick,
            )
            MediaActionIconButton(
                icon = if (isInWatchlist) Icons.Filled.CheckCircle else Icons.Filled.List,
                contentDescription = stringResource(Res.string.watchlist_content_description),
                colors = colors,
                onClick = onWatchlistClick,
            )
            if (showAddToList) {
                MediaActionIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(Res.string.action_add_to_list),
                    colors = colors,
                    onClick = onAddToListClick,
                )
            }
        }
    }
}

/**
 * The wiring layer between a screen's `ViewModel` and the pure [MediaActionButtons] above - the
 * one piece every hero section (`HeroActionRow`, `TvActionRow`) would otherwise have to duplicate
 * identically (see code-conventions §2c): resolve the signed-in session via the shared
 * [AuthScreenModel], hide the row entirely when signed out, collect [mediaActionsState]'s
 * `StateFlow` into plain booleans, and turn each click into a call on [mediaActionsState] - the
 * `ViewModel`-owned state holder built by `MovieDetailScreenModel`/`TvDetailScreenModel` (see
 * [MediaActionsState]'s kdoc). [MediaActionButtons] itself never sees [mediaActionsState] or any
 * repository.
 */
@Composable
internal fun MediaActionButtonsSection(
    mediaId: Long,
    colors: HeroColors,
    showAddToList: Boolean,
    authRepository: AuthRepository,
    mediaActionsState: MediaActionsState,
    modifier: Modifier = Modifier,
    listsRepository: ListsRepository = ListsRepositoryImpl(),
) {
    val authScreenModel =
        viewModel(key = AuthScreenModelDefaults.SHARED_KEY) { AuthScreenModel(authRepository) }
    val authUiState by authScreenModel.uiState.collectAsState()
    val session = (authUiState as? AuthUiState.LoggedIn)?.session ?: return

    val actionsUiState by mediaActionsState.uiState.collectAsState()
    var showAddToListDialog by remember { mutableStateOf(false) }

    MediaActionButtons(
        isFavorite = actionsUiState.isFavorite,
        isInWatchlist = actionsUiState.isInWatchlist,
        showAddToList = showAddToList,
        isLoading = actionsUiState.isLoading,
        colors = colors,
        onFavoriteClick = { mediaActionsState.toggleFavorite(session.accountId, session.sessionId) },
        onWatchlistClick = { mediaActionsState.toggleWatchlist(session.accountId, session.sessionId) },
        onAddToListClick = { showAddToListDialog = true },
        modifier = modifier,
    )

    if (showAddToListDialog) {
        AddToListDialog(
            session = session,
            movieId = mediaId,
            listsRepository = listsRepository,
            onDismiss = { showAddToListDialog = false },
        )
    }
}

/** One icon button's worth of shimmer, shown in place of [MediaActionIconButton] while loading. */
@Composable
private fun MediaActionShimmerButton() {
    Box(
        modifier =
            Modifier
                .size(MediaActionButtonsConstant.BUTTON_SIZE)
                .clip(RoundedCornerShape(MediaActionButtonsConstant.CORNER_RADIUS))
                .addShimmerLoadingAnimation(),
    )
}

@Composable
private fun MediaActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    colors: HeroColors,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(MediaActionButtonsConstant.BUTTON_SIZE)
                .clip(RoundedCornerShape(MediaActionButtonsConstant.CORNER_RADIUS))
                .background(colors.buttonSurface)
                .border(1.dp, colors.buttonOutline, RoundedCornerShape(MediaActionButtonsConstant.CORNER_RADIUS))
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = colors.onHero)
    }
}
