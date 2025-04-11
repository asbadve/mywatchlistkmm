package com.ajinkyabadve.kmmmywatchlist.design.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_surface
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_light_surface

@Composable
fun scrollableChips(
    selectedChip: Int,
    chipItemList: List<String>,
    onClick: (index: Int) -> Unit,
    isLoadingState: Boolean = false,
) {
    val selectionColor: @Composable (Boolean) -> Color = { selection ->
        if (selection) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    }
    LazyRow(
        modifier = Modifier.background(if (isSystemInDarkTheme()) md_theme_dark_surface else md_theme_light_surface),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        itemsIndexed(chipItemList) { index, item ->
            ElevatedAssistChip(
                onClick = { if (!isLoadingState) onClick(index) },
                label = { Text(item) },
                colors =
                    if (isLoadingState) {
                        AssistChipDefaults.assistChipColors(
                            containerColor =
                                selectionColor(
                                    false,
                                ),
                        )
                    } else {
                        AssistChipDefaults.assistChipColors(
                            containerColor =
                                selectionColor(
                                    selectedChip == index,
                                ),
                        )
                    },
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}
