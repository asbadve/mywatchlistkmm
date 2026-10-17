package com.ajinkyabadve.kmmmywatchlist.design.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A tappable settings row: label, optional explanation underneath, optional current [value] and a
 * trailing chevron saying "this opens something".
 *
 * Built on Material 3's [ListItem] rather than a hand-made `Row`, so the typography (bodyLarge
 * headline, bodyMedium supporting text) and minimum height follow the M3 list spec. The container
 * is transparent so the row sits on the screen's own background.
 *
 * The label uses `onBackground` rather than ListItem's default `onSurface`: these rows sit on the
 * background, and in this app's dark palette `onSurface` is almost the same color as the
 * description's `onSurfaceVariant` (#C5C7C2 vs #C0C9BF), which flattened label and description
 * into one tone.
 *
 * @param labelColor overrides the label's color - e.g. `colorScheme.error` for a destructive row
 *   such as "Log out".
 */
@Composable
fun SettingsRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    value: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    ListItem(
        headlineContent = { Text(label) },
        modifier = modifier.clickable(onClick = onClick),
        supportingContent = description?.let { { Text(it) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                value?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = SettingsRowConstant.VALUE_END_PADDING),
                    )
                }
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = labelColor),
    )
}

/**
 * A boolean settings row - a trailing [Switch] instead of [SettingsRow]'s chevron, with an optional
 * explanation of what the toggle does.
 *
 * The whole row is the toggle target (`Modifier.toggleable` with [Role.Switch]) and the [Switch]
 * itself is display-only, so a tap anywhere on the row flips it and accessibility services
 * announce one switch rather than a row plus a separate control. Label color as in [SettingsRow].
 */
@Composable
fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    ListItem(
        headlineContent = { Text(label) },
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        supportingContent = description?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors =
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.onBackground,
            ),
    )
}

private object SettingsRowConstant {
    val VALUE_END_PADDING = 8.dp
}
