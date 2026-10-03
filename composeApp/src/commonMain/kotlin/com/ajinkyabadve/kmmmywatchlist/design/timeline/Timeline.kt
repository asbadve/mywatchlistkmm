package com.ajinkyabadve.kmmmywatchlist.design.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private object TimelineConstant {
    val HORIZONTAL_PADDING = 16.dp
    val RAIL_WIDTH = 24.dp
    val LINE_WIDTH = 2.dp
    val DOT_SIZE = 14.dp
    val DOT_GLOW_SIZE = 24.dp
    const val DOT_GLOW_ALPHA = 0.25f
    val DOT_BORDER_WIDTH = 3.dp
    val SECTION_HEADER_HEIGHT = 40.dp
    val SECTION_TITLE_START_PADDING = 12.dp
    val CARD_CORNER_RADIUS = 16.dp
    val CARD_BORDER_WIDTH = 1.dp
    const val CARD_HIGHLIGHT_BORDER_ALPHA = 0.35f
    val CARD_RAIL_GAP = 12.dp
    val CARD_VERTICAL_SPACING = 5.dp
    val CARD_INNER_PADDING_HORIZONTAL = 12.dp
    val CARD_INNER_PADDING_VERTICAL = 10.dp
}

/*
 * A vertical timeline: a continuous rail down the left with a dot per entry, section headers the
 * rail runs straight through, and each entry's content in a card to the right of the rail. Stack
 * TimelineSectionHeader/TimelineEntryCard items in a LazyColumn with no item spacing and the rail
 * segments meet into one unbroken line.
 *
 * Material3/Compose has no timeline component; this is built from plain layout primitives.
 */

/**
 * The line-and-dot rail for one entry. [highlighted] gives the dot the primary colour and a soft
 * glow, for the one entry the screen is pointing at. Needs a bounded height to fill - its parent
 * must have a definite height, not an intrinsic one.
 */
@Composable
fun TimelineRail(
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(TimelineConstant.RAIL_WIDTH).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TimelineLineSegment(modifier = Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            if (highlighted) {
                Box(
                    modifier =
                        Modifier
                            .size(TimelineConstant.DOT_GLOW_SIZE)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = TimelineConstant.DOT_GLOW_ALPHA), CircleShape),
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(TimelineConstant.DOT_SIZE)
                        .background(
                            if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        )
                        // A ring in the surface colour separating the dot from the line behind it.
                        .border(TimelineConstant.DOT_BORDER_WIDTH, MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
        TimelineLineSegment(modifier = Modifier.weight(1f))
    }
}

/** A section title (e.g. a month) with the rail running through it uninterrupted. */
@Composable
fun TimelineSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TimelineConstant.SECTION_HEADER_HEIGHT)
                .padding(horizontal = TimelineConstant.HORIZONTAL_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(TimelineConstant.RAIL_WIDTH).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            TimelineLineSegment(modifier = Modifier.fillMaxHeight())
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = TimelineConstant.SECTION_TITLE_START_PADDING),
        )
    }
}

/**
 * One timeline entry: the [TimelineRail] on the left and [content] in a bordered card to its
 * right. [contentHeight] is the height of the tallest thing in [content]; the entry's own height is
 * derived from it so the rail gets the definite height it needs and adjacent entries' rails touch.
 */
@Composable
fun TimelineEntryCard(
    contentHeight: Dp,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val cardShape = RoundedCornerShape(TimelineConstant.CARD_CORNER_RADIUS)
    val borderColor =
        if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = TimelineConstant.CARD_HIGHLIGHT_BORDER_ALPHA)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    val entryHeight = contentHeight + (TimelineConstant.CARD_INNER_PADDING_VERTICAL + TimelineConstant.CARD_VERTICAL_SPACING) * 2

    // No vertical padding on this outer Row: the rail spans its full height so neighbouring
    // entries' rails meet. The gap between cards is the card's own padding, which the rail is
    // outside of.
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(entryHeight)
                .padding(horizontal = TimelineConstant.HORIZONTAL_PADDING),
    ) {
        TimelineRail(highlighted = highlighted)
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(
                        start = TimelineConstant.CARD_RAIL_GAP,
                        top = TimelineConstant.CARD_VERTICAL_SPACING,
                        bottom = TimelineConstant.CARD_VERTICAL_SPACING,
                    ).clip(cardShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(TimelineConstant.CARD_BORDER_WIDTH, borderColor, cardShape)
                    .clickable(onClick = onClick)
                    .padding(
                        horizontal = TimelineConstant.CARD_INNER_PADDING_HORIZONTAL,
                        vertical = TimelineConstant.CARD_INNER_PADDING_VERTICAL,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun TimelineLineSegment(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .width(TimelineConstant.LINE_WIDTH)
                .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
