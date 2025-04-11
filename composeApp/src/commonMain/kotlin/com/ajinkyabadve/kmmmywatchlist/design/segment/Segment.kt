package com.ajinkyabadve.kmmmywatchlist.design.segment

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SingleChoiceSegment(
    modifier: Modifier,
    onOptionSelected: (Int) -> Unit,
    options: List<String>,
    selectedIndex: Int,
) {
    InternalSegment(
        modifier = Modifier,
        options = options,
        selectedIndex = selectedIndex,
        onOptionSelected = onOptionSelected,
    )
}

@Composable
private fun InternalSegment(
    modifier: Modifier,
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                onClick = { onOptionSelected.invoke(index) },
                selected = index == selectedIndex,
                label = { Text(label) },
            )
        }
    }
}
