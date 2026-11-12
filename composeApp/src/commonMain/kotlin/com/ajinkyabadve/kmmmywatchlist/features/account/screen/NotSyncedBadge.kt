package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.UploadCloud
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.not_synced_content_description
import org.jetbrains.compose.resources.stringResource

/** Not `private`: UI tests find the marker by this tag. */
object NotSyncedBadgeConstant {
    const val TAG = "NotSyncedBadge"
}

private object NotSyncedBadgeStyle {
    val ICON_SIZE = 14.dp
    val PADDING = 5.dp

    /** Same translucency as `UpcomingBadge`, so the two corner badges read as one family. */
    const val ALPHA = 0.85f
}

/**
 * Corner marker on a Favorites/Watchlist poster added offline and not yet on TMDB. An upload-cloud
 * icon - the same idea as Google Photos' "not backed up yet" cloud on a thumbnail - and quiet
 * colours, not error colours: the change is safe locally, just not sent yet (Google's offline &
 * sync design guidelines: connectivity status should not look like an error). It disappears on
 * its own once the change reaches TMDB. Material3 has `Badge`, but it is a small dot/number for
 * counts on navigation items, not an icon marker on content, so this mirrors `UpcomingBadge`.
 */
@Composable
fun NotSyncedBadge(modifier: Modifier = Modifier) {
    Icon(
        imageVector = FeatherIcons.UploadCloud,
        contentDescription = stringResource(Res.string.not_synced_content_description),
        tint = MaterialTheme.colorScheme.inverseOnSurface,
        modifier =
            modifier
                .testTag(NotSyncedBadgeConstant.TAG)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = NotSyncedBadgeStyle.ALPHA))
                .padding(NotSyncedBadgeStyle.PADDING)
                .size(NotSyncedBadgeStyle.ICON_SIZE),
    )
}
