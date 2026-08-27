package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

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
import mywatchlist.composeapp.generated.resources.follow_person_content_description
import mywatchlist.composeapp.generated.resources.follow_person_label
import mywatchlist.composeapp.generated.resources.follow_person_local_only_caveat
import mywatchlist.composeapp.generated.resources.following_person_label
import org.jetbrains.compose.resources.stringResource

private object FollowPersonButtonConstant {
    val CORNER_RADIUS = 8.dp
    val ICON_TEXT_GAP = 6.dp
    val HORIZONTAL_PADDING = 12.dp
    val VERTICAL_PADDING = 8.dp
    val CAVEAT_TOP_PADDING = 4.dp
    val CAVEAT_FONT_SIZE = 11.sp
    const val CAVEAT_TEXT_ALPHA = 0.7f
}

/**
 * "Favorite this person" pill for [PersonHeroSection] - local-only, unlike `MediaActionButtons`'
 * favorite/watchlist icons: TMDB has no account-level favorite/follow API for people (confirmed
 * 2026-08-26, see `FavoritePersonRepository`'s kdoc), so there's no `account_states` pre-check to
 * gate this behind a signed-in session, and no shimmer-while-loading state either - [isFollowing]
 * is a plain local SQLite read, always immediately known.
 *
 * Pure/reusable per code-conventions §7/§8: plain [isFollowing] boolean + [onToggleClick] callback,
 * no repository reference - `PersonDetailScreen` (the sole caller, via [PersonHeroSection]) is the
 * one place that owns [com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepository]
 * through `PersonDetailScreenModel`.
 *
 * Once followed, shows a small caveat that this is device-local (requested 2026-08-26): unlike
 * movie/TV favorites, nothing here is tied to the signed-in TMDB session, so it never appears on
 * another device. Shown only while [isFollowing] rather than always, so a never-followed person's
 * page isn't cluttered with a disclaimer about a state the user hasn't opted into yet.
 */
@Composable
internal fun FollowPersonButton(
    isFollowing: Boolean,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(FollowPersonButtonConstant.CORNER_RADIUS))
                    .clickable(onClick = onToggleClick)
                    .padding(
                        horizontal = FollowPersonButtonConstant.HORIZONTAL_PADDING,
                        vertical = FollowPersonButtonConstant.VERTICAL_PADDING,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isFollowing) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(Res.string.follow_person_content_description),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(if (isFollowing) Res.string.following_person_label else Res.string.follow_person_label),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = FollowPersonButtonConstant.ICON_TEXT_GAP),
            )
        }
        if (isFollowing) {
            Text(
                text = stringResource(Res.string.follow_person_local_only_caveat),
                fontSize = FollowPersonButtonConstant.CAVEAT_FONT_SIZE,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = FollowPersonButtonConstant.CAVEAT_TEXT_ALPHA),
                modifier =
                    Modifier.padding(
                        start = FollowPersonButtonConstant.HORIZONTAL_PADDING,
                        top = FollowPersonButtonConstant.CAVEAT_TOP_PADDING,
                    ),
            )
        }
    }
}
