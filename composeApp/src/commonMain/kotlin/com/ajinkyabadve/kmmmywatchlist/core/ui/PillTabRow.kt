package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private object PillTabRowConstant {
    val TABS_WIDTH = 400.dp
    val INDICATOR_HORIZONTAL_PADDING = 24.dp
    val INDICATOR_HEIGHT = 4.dp
    val INDICATOR_CORNER_RADIUS = 4.dp
}

/**
 * The centered, colored-underline scrollable tab row shared by every top-level tab switcher in the
 * app (movie category tabs, the My Fav tabs) - extracted so the visual can't drift between them.
 * Owns only the row chrome; callers own selection state and what renders below it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val calculatedPadding = (maxWidth - PillTabRowConstant.TABS_WIDTH) / 2
        val edgePadding = if (calculatedPadding > 0.dp) calculatedPadding else 0.dp

        PrimaryScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = edgePadding,
            modifier = Modifier,
            containerColor = Color.Transparent,
            indicator = {
                Box(
                    modifier =
                        Modifier
                            .tabIndicatorOffset(selectedIndex)
                            .padding(horizontal = PillTabRowConstant.INDICATOR_HORIZONTAL_PADDING)
                            .height(PillTabRowConstant.INDICATOR_HEIGHT)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape =
                                    RoundedCornerShape(
                                        topStart = PillTabRowConstant.INDICATOR_CORNER_RADIUS,
                                        topEnd = PillTabRowConstant.INDICATOR_CORNER_RADIUS,
                                    ),
                            ),
                )
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) },
                )
            }
        }
    }
}
