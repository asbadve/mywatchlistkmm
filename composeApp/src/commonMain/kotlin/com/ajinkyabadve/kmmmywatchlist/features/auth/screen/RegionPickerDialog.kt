package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.format.toRegionFlagEmoji
import com.ajinkyabadve.kmmmywatchlist.features.settings.model.WatchProviderRegion
import com.ajinkyabadve.kmmmywatchlist.features.settings.screen.RegionLoadState
import com.ajinkyabadve.kmmmywatchlist.features.settings.screen.RegionScreenModel
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.region_empty_message
import mywatchlist.composeapp.generated.resources.region_search_hint
import org.jetbrains.compose.resources.stringResource

private object RegionPickerDialogConstant {
    val MAX_HEIGHT = 400.dp
}

/**
 * Region picker for the account settings rows - search + list, mirrors [AddToListDialog]'s shape.
 *
 * Which setting the pick is written to (selected region vs. default fallback region) is entirely
 * the caller's decision via [onRegionSelected] - this composable and [RegionScreenModel] only know
 * how to browse/search the available regions, not what a pick means.
 */
@Composable
fun RegionPickerDialog(
    title: String,
    selectedRegionCode: String,
    onRegionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    regionScreenModel: RegionScreenModel = viewModel { RegionScreenModel() },
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = regionScreenModel.searchQuery,
                    onValueChange = regionScreenModel::onSearchQueryChanged,
                    label = { Text(stringResource(Res.string.region_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                when (regionScreenModel.loadState) {
                    is RegionLoadState.Loading -> Unit
                    is RegionLoadState.Loaded -> {
                        if (regionScreenModel.filteredRegions.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.region_empty_message),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = RegionPickerDialogConstant.MAX_HEIGHT)) {
                                items(regionScreenModel.filteredRegions, key = { it.iso3166 }) { region ->
                                    RegionRow(
                                        region = region,
                                        isSelected = region.iso3166 == selectedRegionCode,
                                        onClick = {
                                            onRegionSelected(region.iso3166)
                                            onDismiss()
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}

@Composable
private fun RegionRow(
    region: WatchProviderRegion,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${region.iso3166.toRegionFlagEmoji()} ${region.englishName}".trim(),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = region.iso3166,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
