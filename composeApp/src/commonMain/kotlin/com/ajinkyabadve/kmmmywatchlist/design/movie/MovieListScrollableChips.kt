package com.ajinkyabadve.kmmmywatchlist.design.movie

import androidx.compose.foundation.background
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


@Composable
fun movieListScrollableChips(
    selectedChip: Int,
    chipItemList: List<String>,
    onClick: (index: Int) -> Unit,
) {

    val selectionColor: @Composable (Boolean) -> Color = { selection ->
        if (selection) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    }
    LazyRow(
        modifier = Modifier.background(md_theme_dark_surface),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(chipItemList) { index, item ->
            ElevatedAssistChip(
                onClick = { onClick(index) },
                label = { Text(item) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = selectionColor(
                        selectedChip == index
                    )
                ),
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}
