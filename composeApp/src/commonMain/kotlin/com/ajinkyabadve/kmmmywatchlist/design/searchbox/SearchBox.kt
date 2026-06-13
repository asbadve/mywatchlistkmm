package com.ajinkyabadve.kmmmywatchlist.design.searchbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_secondaryContainer
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_light_secondaryContainer

// TODO: Use the latest https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#SearchBar(androidx.compose.material3.SearchBarState,kotlin.Function0,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Shape,androidx.compose.material3.SearchBarColors,androidx.compose.ui.unit.Dp,androidx.compose.ui.unit.Dp)
@Suppress("ktlint:standard:function-naming", "LongParameterList", "FunctionNaming")
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

@Suppress("ktlint:standard:function-naming", "LongParameterList", "FunctionNaming")
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
        modifier =
            Modifier
                .wrapContentSize(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(end = 16.dp)
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.CenterStart,
            modifier =
                Modifier
                    .height(45.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(getBackgroundColor(isSystemInDarkTheme()), shape)
                    .clickable(
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication =
                            ripple(
                                bounded = true,
                            ),
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
                    modifier =
                        Modifier.padding(start = 0.dp, end = 16.dp)
                            .align(Alignment.CenterVertically),
                    maxLines = 1,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Proportional,
                                    trim = LineHeightStyle.Trim.None,
                                ),
                        ),
                )
            }
        }
    }
}

private fun getBackgroundColor(isDark: Boolean) =
    if (isDark) {
        md_theme_dark_secondaryContainer
    } else {
        md_theme_light_secondaryContainer
    }
