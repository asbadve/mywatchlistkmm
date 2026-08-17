package com.ajinkyabadve.kmmmywatchlist.features.search.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.filter_upcoming
import org.jetbrains.compose.resources.stringResource

/**
 * A quiet corner marker for a watchlist/favorite item that hasn't released yet. Deliberately muted
 * (a neutral inverse-surface pill, not a saturated theme color like [MediaTypeBadge]) since this is
 * a secondary glance-cue on a poster grid, not the primary thing each card communicates.
 */
@Composable
fun UpcomingBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.filter_upcoming),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.inverseOnSurface,
        modifier =
            modifier
                .clip(RoundedCornerShape(BADGE_CORNER_RADIUS))
                .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = BADGE_ALPHA))
                .padding(horizontal = BADGE_HORIZONTAL_PADDING, vertical = BADGE_VERTICAL_PADDING),
    )
}

private val BADGE_CORNER_RADIUS = 6.dp
private val BADGE_HORIZONTAL_PADDING = 6.dp
private val BADGE_VERTICAL_PADDING = 2.dp

/** Slightly translucent so a busy poster still reads through behind the badge. */
private const val BADGE_ALPHA = 0.85f
