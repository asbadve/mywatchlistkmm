package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajinkyabadve.kmmmywatchlist.core.formatClockTime
import com.ajinkyabadve.kmmmywatchlist.core.notification.rememberNotificationPermissionRequester
import com.ajinkyabadve.kmmmywatchlist.is24HourClock
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.reminder_permission_denied_caption
import mywatchlist.composeapp.generated.resources.reminder_time_caption
import mywatchlist.composeapp.generated.resources.time_am
import mywatchlist.composeapp.generated.resources.time_pm
import org.jetbrains.compose.resources.stringResource

private object ReleaseReminderHeroActionConstant {
    val CAPTION_START_PADDING = 12.dp
    val CAPTION_FONT_SIZE = 11.sp
    const val CAPTION_ALPHA = 0.75f
}

/**
 * The hero's "Remind me" control plus its one-line caption. Turning a reminder on asks for
 * notification permission first (the same requester every other opt-in in this app uses) and only
 * saves the reminder if it's granted. Once set, the caption says when it will arrive and that the
 * time can be changed in Settings, so the time preference is discoverable without a separate
 * onboarding step.
 *
 * [onSetReminder] receives `true` to add the reminder and `false` to remove it.
 */
@Composable
fun ReleaseReminderHeroAction(
    isSet: Boolean,
    reminderTime: LocalTime?,
    contentColor: Color,
    onSetReminder: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val permissionRequester = rememberNotificationPermissionRequester()
    val scope = rememberCoroutineScope()
    var permissionDenied by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        ReleaseReminderButton(
            isSet = isSet,
            contentColor = contentColor,
            onToggle = {
                if (isSet) {
                    onSetReminder(false)
                } else {
                    scope.launch {
                        val granted = permissionRequester.request()
                        permissionDenied = !granted
                        if (granted) onSetReminder(true)
                    }
                }
            },
        )
        val caption =
            when {
                isSet && reminderTime != null ->
                    stringResource(
                        Res.string.reminder_time_caption,
                        formatClockTime(
                            reminderTime,
                            is24HourClock(),
                            stringResource(Res.string.time_am),
                            stringResource(Res.string.time_pm),
                        ),
                    )
                permissionDenied -> stringResource(Res.string.reminder_permission_denied_caption)
                else -> null
            }
        if (caption != null) {
            Text(
                text = caption,
                fontSize = ReleaseReminderHeroActionConstant.CAPTION_FONT_SIZE,
                color = contentColor.copy(alpha = ReleaseReminderHeroActionConstant.CAPTION_ALPHA),
                modifier = Modifier.padding(start = ReleaseReminderHeroActionConstant.CAPTION_START_PADDING),
            )
        }
    }
}
