package com.ajinkyabadve.kmmmywatchlist.design.searchbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_secondaryContainer
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_light_secondaryContainer


@Composable
fun SearchBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    hint: String? = "hint text",
    leftImageVector: ImageVector? = Icons.Filled.Search,
    leftIconContentDescription: String? = "Localized description",
    leftIconTint: Color = LocalContentColor.current,
    onClick: () -> Unit,
) {
    InternalSearchBox(
        shape = shape,
        hint = hint ?: "",
        leftIconPainter = rememberVectorPainter(leftImageVector ?: Icons.Filled.Search),
        contentDescription = leftIconContentDescription ?: "",
        modifier = modifier,
        leftIconTint = leftIconTint,
        onClick = onClick,
    )
}

@Composable
private fun InternalSearchBox(
    shape: Shape,
    hint: String,
    leftIconPainter: Painter?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    leftIconTint: Color = LocalContentColor.current,
    onClick: () -> Unit,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },

    ) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier.height(45.dp)
            .wrapContentSize(Alignment.CenterStart)
            .wrapContentWidth()
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = true,
                ),
            ),
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.CenterStart,
            modifier = modifier.fillMaxHeight()
                .clip(shape)
                .background(
                    if (isSystemInDarkTheme()) md_theme_dark_secondaryContainer else md_theme_light_secondaryContainer
                ),
        ) {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leftIconPainter != null) {
                    IconButton(modifier = Modifier, onClick = { /*TODO*/ }) {
                        Icon(
                            painter = leftIconPainter,
                            contentDescription = contentDescription,
                            tint = leftIconTint,
                        )
                    }
                }
                Text(
                    hint ?: "",
                    modifier = Modifier.padding(start = 0.dp, end = 16.dp),
                    maxLines = 1,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
