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
import com.ajinkyabadve.kmmmywatchlist.features.search.model.SearchMediaType
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.media_type_movie
import mywatchlist.composeapp.generated.resources.media_type_person
import mywatchlist.composeapp.generated.resources.media_type_tv
import org.jetbrains.compose.resources.stringResource

/**
 * The per-card "which of the three things is this" marker. `/3/search/multi` interleaves movies, TV
 * shows and people in one relevance-ordered list, and a poster alone doesn't say which - a person's
 * headshot and a film poster read the same at grid size.
 */
@Composable
fun MediaTypeBadge(
    mediaType: SearchMediaType,
    modifier: Modifier = Modifier,
) {
    val label =
        when (mediaType) {
            SearchMediaType.MOVIE -> stringResource(Res.string.media_type_movie)
            SearchMediaType.TV -> stringResource(Res.string.media_type_tv)
            SearchMediaType.PERSON -> stringResource(Res.string.media_type_person)
        }
    val containerColor =
        when (mediaType) {
            SearchMediaType.MOVIE -> MaterialTheme.colorScheme.primary
            SearchMediaType.TV -> MaterialTheme.colorScheme.tertiary
            SearchMediaType.PERSON -> MaterialTheme.colorScheme.secondary
        }
    val contentColor =
        when (mediaType) {
            SearchMediaType.MOVIE -> MaterialTheme.colorScheme.onPrimary
            SearchMediaType.TV -> MaterialTheme.colorScheme.onTertiary
            SearchMediaType.PERSON -> MaterialTheme.colorScheme.onSecondary
        }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        modifier =
            modifier
                .clip(RoundedCornerShape(BADGE_CORNER_RADIUS))
                .background(containerColor.copy(alpha = BADGE_ALPHA))
                .padding(horizontal = BADGE_HORIZONTAL_PADDING, vertical = BADGE_VERTICAL_PADDING),
    )
}

private val BADGE_CORNER_RADIUS = 6.dp
private val BADGE_HORIZONTAL_PADDING = 6.dp
private val BADGE_VERTICAL_PADDING = 2.dp

/** Slightly translucent so a busy poster still reads through behind the badge. */
private const val BADGE_ALPHA = 0.85f
