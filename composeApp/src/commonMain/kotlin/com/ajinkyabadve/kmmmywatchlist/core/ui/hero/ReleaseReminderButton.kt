package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_remind_me
import mywatchlist.composeapp.generated.resources.action_reminder_set
import org.jetbrains.compose.resources.stringResource

private object ReleaseReminderButtonConstant {
    val CORNER_RADIUS = 8.dp
    val ICON_TEXT_GAP = 6.dp
    val HORIZONTAL_PADDING = 12.dp
    val VERTICAL_PADDING = 8.dp
}

/**
 * "Remind me" toggle for an upcoming movie or show (checklist item 16). Lives in the hero next to
 * `MediaActionButtonsSection`, not inside it, because that section renders nothing while signed
 * out and a release reminder is purely local - it needs no TMDB account, like following a
 * collection (`FollowCollectionButton`, which this is modelled on).
 *
 * Pure per code-conventions §7/§8: plain [isSet] + [onToggle]; the screen model owns the
 * repository, permission request and scheduling.
 */
@Composable
fun ReleaseReminderButton(
    isSet: Boolean,
    onToggle: () -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(ReleaseReminderButtonConstant.CORNER_RADIUS))
                .clickable(onClick = onToggle)
                .padding(
                    horizontal = ReleaseReminderButtonConstant.HORIZONTAL_PADDING,
                    vertical = ReleaseReminderButtonConstant.VERTICAL_PADDING,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val label = stringResource(if (isSet) Res.string.action_reminder_set else Res.string.action_remind_me)
        Icon(
            imageVector = if (isSet) Icons.Filled.Notifications else Icons.Outlined.Notifications,
            contentDescription = null,
            tint = contentColor,
        )
        Text(
            text = label,
            color = contentColor,
            modifier = Modifier.padding(start = ReleaseReminderButtonConstant.ICON_TEXT_GAP),
        )
    }
}
