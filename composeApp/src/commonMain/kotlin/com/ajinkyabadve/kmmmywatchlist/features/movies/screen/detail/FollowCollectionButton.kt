package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.follow_collection_content_description
import mywatchlist.composeapp.generated.resources.follow_collection_label
import mywatchlist.composeapp.generated.resources.follow_collection_local_only_caveat
import mywatchlist.composeapp.generated.resources.following_collection_label
import org.jetbrains.compose.resources.stringResource

private object FollowCollectionButtonConstant {
    val CORNER_RADIUS = 8.dp
    val ICON_TEXT_GAP = 6.dp
    val HORIZONTAL_PADDING = 12.dp
    val VERTICAL_PADDING = 8.dp
    val CAVEAT_TOP_PADDING = 4.dp
    val CAVEAT_FONT_SIZE = 11.sp
    const val CAVEAT_TEXT_ALPHA = 0.7f
}

/**
 * "Favorite this collection" pill for `CollectionHeader` - local-only, unlike `MediaActionButtons`'
 * favorite/watchlist icons: TMDB has no account-level favorite/follow API for collections either
 * (confirmed against the live OpenAPI docs, same finding as
 * [com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail.FollowPersonButton]'s kdoc for
 * people), so there's no `account_states` pre-check to gate this behind a signed-in session, and
 * no shimmer-while-loading state either - [isFollowing] is a plain local SQLite read, always
 * immediately known.
 *
 * Pure/reusable per code-conventions §7/§8: plain [isFollowing] boolean + [onToggleClick] callback,
 * no repository reference - `CollectionDetailScreen` (the sole caller) is the one place that owns
 * [com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepository]
 * through `CollectionDetailScreenModel`.
 *
 * Once followed, shows a small caveat that this is device-local, same reasoning as
 * `FollowPersonButton`'s identical caveat: nothing here is tied to the signed-in TMDB session, so
 * it never appears on another device.
 */
@Composable
internal fun FollowCollectionButton(
    isFollowing: Boolean,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(FollowCollectionButtonConstant.CORNER_RADIUS))
                    .clickable(onClick = onToggleClick)
                    .padding(
                        horizontal = FollowCollectionButtonConstant.HORIZONTAL_PADDING,
                        vertical = FollowCollectionButtonConstant.VERTICAL_PADDING,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isFollowing) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(Res.string.follow_collection_content_description),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(if (isFollowing) Res.string.following_collection_label else Res.string.follow_collection_label),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = FollowCollectionButtonConstant.ICON_TEXT_GAP),
            )
        }
        if (isFollowing) {
            Text(
                text = stringResource(Res.string.follow_collection_local_only_caveat),
                fontSize = FollowCollectionButtonConstant.CAVEAT_FONT_SIZE,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = FollowCollectionButtonConstant.CAVEAT_TEXT_ALPHA),
                modifier =
                    Modifier.padding(
                        start = FollowCollectionButtonConstant.HORIZONTAL_PADDING,
                        top = FollowCollectionButtonConstant.CAVEAT_TOP_PADDING,
                    ),
            )
        }
    }
}
