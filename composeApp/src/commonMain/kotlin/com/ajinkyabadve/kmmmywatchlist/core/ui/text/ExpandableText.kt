package com.ajinkyabadve.kmmmywatchlist.core.ui.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_read_less
import mywatchlist.composeapp.generated.resources.action_read_more
import org.jetbrains.compose.resources.stringResource

/**
 * Text cut to [collapsedMaxLines] with a "Read more" / "Read less" toggle under it, like
 * themoviedb.org. The toggle only shows when the collapsed text actually overflows.
 *
 * Neither Compose nor Material3 ships an expandable text component; the platform pieces are
 * `maxLines` plus `TextLayoutResult.hasVisualOverflow`, which is what this wraps. Shared by the
 * person biography and the TV current-season overview so both behave the same.
 *
 * While expanded, tapping the text itself also collapses it, so a long passage can be closed
 * without scrolling back down to "Read less". While collapsed, a tap on the text is left to the
 * parent: the current-season row opens the season list from it.
 *
 * The toggle has its own click target, so it works inside a parent that is itself clickable.
 */
@Composable
fun ExpandableText(
    text: String,
    collapsedMaxLines: Int,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var hasOverflow by rememberSaveable(text) { mutableStateOf(false) }
    val readLessLabel = stringResource(Res.string.action_read_less)
    Column(modifier = modifier) {
        Text(
            text = text,
            modifier =
                if (expanded) {
                    Modifier.clickable(onClickLabel = readLessLabel, role = Role.Button) { expanded = false }
                } else {
                    Modifier
                },
            fontSize = fontSize,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult ->
                if (!expanded) hasOverflow = layoutResult.hasVisualOverflow
            },
            color = color,
        )
        if (hasOverflow || expanded) {
            Text(
                text = if (expanded) readLessLabel else stringResource(Res.string.action_read_more),
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .clickable(role = Role.Button) { expanded = !expanded },
            )
        }
    }
}
