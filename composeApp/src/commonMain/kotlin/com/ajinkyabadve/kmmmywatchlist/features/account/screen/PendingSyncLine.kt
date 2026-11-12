package com.ajinkyabadve.kmmmywatchlist.features.account.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.UploadCloud
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.pending_sync_waiting
import org.jetbrains.compose.resources.pluralStringResource

/** Not `private`: UI tests find the line by this tag. */
object PendingSyncLineConstant {
    const val TAG = "PendingSyncLine"
}

private object PendingSyncLineStyle {
    val CORNER_RADIUS = 12.dp
    val ICON_SIZE = 16.dp
}

/**
 * One quiet line above the Favorites/Watchlist grid while [waitingCount] offline changes wait to
 * reach TMDB - the global half of the "not synced" UI, next to [NotSyncedBadge] on each poster.
 * It is also the only place an offline removal shows, since that title is already off the grid.
 * Follows Google's offline & sync design guidelines: a grey surface, not error red; reassuring copy
 * ("sent automatically once you're online"); no retry countdown or spinner. Shown only while
 * something is waiting - callers skip it at zero.
 */
@Composable
fun PendingSyncLine(
    waitingCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(PendingSyncLineStyle.CORNER_RADIUS),
        modifier = modifier.fillMaxWidth().testTag(PendingSyncLineConstant.TAG),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            // Decorative: the text says the same thing.
            Icon(
                imageVector = FeatherIcons.UploadCloud,
                contentDescription = null,
                modifier = Modifier.size(PendingSyncLineStyle.ICON_SIZE),
            )
            Text(
                text = pluralStringResource(Res.plurals.pending_sync_waiting, waitingCount, waitingCount),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
