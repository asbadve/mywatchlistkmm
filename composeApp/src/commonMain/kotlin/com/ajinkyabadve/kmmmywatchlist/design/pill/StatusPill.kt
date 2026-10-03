package com.ajinkyabadve.kmmmywatchlist.design.pill

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private object StatusPillConstant {
    const val CORNER_PERCENT = 50
    val HORIZONTAL_PADDING = 10.dp
    val VERTICAL_PADDING = 4.dp
}

/**
 * A small, non-interactive rounded label (e.g. "Tomorrow", "In 3 days"). [highlighted] fills it
 * with the primary colour to mark the one item that matters most.
 *
 * Material3's SuggestionChip/AssistChip were the platform candidates, but both are interactive
 * (required onClick, 32dp minimum touch height, ripple), and Badge is sized for a count, not text.
 */
@Composable
fun StatusPill(
    text: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier
                .clip(RoundedCornerShape(StatusPillConstant.CORNER_PERCENT))
                .background(if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = StatusPillConstant.HORIZONTAL_PADDING, vertical = StatusPillConstant.VERTICAL_PADDING),
    )
}
