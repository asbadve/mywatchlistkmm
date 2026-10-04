package com.ajinkyabadve.kmmmywatchlist.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.getSystemService
import com.ajinkyabadve.kmmmywatchlist.AndroidApp
import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderCoordinator
import com.ajinkyabadve.kmmmywatchlist.features.notifications.toEpochMillis
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.minutes

internal object AndroidReminderSchedulerConstant {
    const val TAG = "AndroidReminderScheduler"

    // Each reminder's id goes into the Intent's data URI. Intent equality (which is what
    // PendingIntent matching uses) ignores extras, so without this every reminder would share one
    // PendingIntent and replace the previous one.
    const val ID_URI_SCHEME = "mywatchlist-reminder"
    const val ID_URI_SEPARATOR = "://"

    const val EXTRA_NOTIFICATION_ID = "reminder_notification_id"
    const val EXTRA_TITLE = "reminder_title"
    const val EXTRA_BODY = "reminder_body"
    const val EXTRA_POSTER_URL = "reminder_poster_url"
    const val EXTRA_MEDIA_ID = "reminder_media_id"
    const val EXTRA_MEDIA_TYPE = "reminder_media_type"
    const val NO_MEDIA_ID = -1L
}

actual fun platformReminderScheduler(): ReminderScheduler = AndroidReminderScheduler

/**
 * Android's [ReminderScheduler]: an inexact `AlarmManager.setWindow` alarm (no exact-alarm
 * permission needed - see [ReminderScheduler]'s kdoc) that fires [ReleaseReminderReceiver], which
 * posts through the existing [LocalNotifier]. Alarms don't survive a reboot, an app update or a
 * force-stop; [ReminderRescheduleReceiver] and the app-start reschedule put them back from the
 * database.
 */
internal object AndroidReminderScheduler : ReminderScheduler {
    override suspend fun schedule(
        id: String,
        at: LocalDateTime,
        content: ReminderContent,
        windowMinutes: Int,
    ) {
        val context = AndroidApp.instance
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                reminderIntent(context, id).putContent(content),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            at.toEpochMillis(TimeZone.currentSystemDefault()),
            windowMinutes.minutes.inWholeMilliseconds,
            pendingIntent,
        )
    }

    override suspend fun cancel(ids: Collection<String>) {
        val context = AndroidApp.instance
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        ids.forEach { id ->
            existingPendingIntent(context, id)?.let { pendingIntent ->
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    override suspend fun pendingForDebug(expected: Map<String, LocalDateTime>): List<PendingReminder> {
        val context = AndroidApp.instance
        return expected.map { (id, fireAt) ->
            PendingReminder(id = id, fireAt = fireAt, isRegistered = existingPendingIntent(context, id) != null)
        }
    }

    private fun existingPendingIntent(
        context: Context,
        id: String,
    ): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            0,
            reminderIntent(context, id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun reminderIntent(
        context: Context,
        id: String,
    ): Intent =
        Intent(context, ReleaseReminderReceiver::class.java)
            .setData(Uri.parse(AndroidReminderSchedulerConstant.ID_URI_SCHEME + AndroidReminderSchedulerConstant.ID_URI_SEPARATOR + id))

    private fun Intent.putContent(content: ReminderContent): Intent {
        putExtra(AndroidReminderSchedulerConstant.EXTRA_NOTIFICATION_ID, content.notificationId)
        putExtra(AndroidReminderSchedulerConstant.EXTRA_TITLE, content.title)
        putExtra(AndroidReminderSchedulerConstant.EXTRA_BODY, content.body)
        putExtra(AndroidReminderSchedulerConstant.EXTRA_POSTER_URL, content.posterUrl)
        val target = content.target
        if (target is MediaDetailNotificationTarget) {
            putExtra(AndroidReminderSchedulerConstant.EXTRA_MEDIA_ID, target.mediaId)
            putExtra(AndroidReminderSchedulerConstant.EXTRA_MEDIA_TYPE, target.mediaType)
        }
        return this
    }
}

/** Fires when a reminder's alarm goes off and posts it through [LocalNotifier], so it looks and
 *  taps exactly like every other notification this app posts. */
class ReleaseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val title = intent.getStringExtra(AndroidReminderSchedulerConstant.EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(AndroidReminderSchedulerConstant.EXTRA_BODY).orEmpty()
        val mediaId = intent.getLongExtra(AndroidReminderSchedulerConstant.EXTRA_MEDIA_ID, AndroidReminderSchedulerConstant.NO_MEDIA_ID)
        val mediaType = intent.getStringExtra(AndroidReminderSchedulerConstant.EXTRA_MEDIA_TYPE) ?: MediaTypeConstant.MOVIE
        val target =
            if (mediaId ==
                AndroidReminderSchedulerConstant.NO_MEDIA_ID
            ) {
                null
            } else {
                MediaDetailNotificationTarget(mediaId, mediaType)
            }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                LocalNotifier.post(
                    notificationId = intent.getIntExtra(AndroidReminderSchedulerConstant.EXTRA_NOTIFICATION_ID, title.hashCode()),
                    title = title,
                    body = body,
                    deepLink = target,
                    posterUrl = intent.getStringExtra(AndroidReminderSchedulerConstant.EXTRA_POSTER_URL),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Puts every reminder alarm back after something that wipes them: a reboot, an app update, or
 *  a change to the device clock or time zone (which moves when "09:00 local" is). */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Napier.d(tag = AndroidReminderSchedulerConstant.TAG) { "Rescheduling reminders after ${intent.action}" }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ReleaseReminderCoordinator().rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
