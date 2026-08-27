@file:JvmName("LocalNotifierAndroid")

package com.ajinkyabadve.kmmmywatchlist.core.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ajinkyabadve.kmmmywatchlist.AndroidApp
import com.ajinkyabadve.kmmmywatchlist.AppActivity
import com.ajinkyabadve.kmmmywatchlist.R

internal object AndroidNotificationConstant {
    const val CHANNEL_ID = "episode_notifications"

    // Read back by AppActivity.handleNotificationIntent() - keep both sides in sync.
    const val EXTRA_TV_SHOW_ID = "notification_tv_show_id"
    const val EXTRA_SEASON_NUMBER = "notification_season_number"
    const val EXTRA_EPISODE_NUMBER = "notification_episode_number"
    const val EXTRA_PERSON_ID = "notification_person_id"

    // Prefix, not just the raw id, so this can never collide with a per-reason notificationId
    // (Triple(id, mediaType, reason).hashCode() in TvEpisodeNotificationPoller) that happens to
    // equal a tvShowId. A distinct prefix per NotificationTarget kind (not just one shared prefix)
    // also stops a person id and a tvShowId that happen to be numerically equal from merging into
    // the same stack.
    const val GROUP_KEY_PREFIX = "episode_notifications_group_"
    const val PERSON_GROUP_KEY_PREFIX = "person_notifications_group_"
}

actual object LocalNotifier {
    actual suspend fun post(
        notificationId: Int,
        title: String,
        body: String,
        deepLink: NotificationTarget?,
        posterUrl: String?,
    ) {
        val context = AndroidApp.instance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val contentIntent =
            Intent(context, AppActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                when (deepLink) {
                    is EpisodeNotificationTarget -> {
                        putExtra(AndroidNotificationConstant.EXTRA_TV_SHOW_ID, deepLink.tvShowId)
                        putExtra(AndroidNotificationConstant.EXTRA_SEASON_NUMBER, deepLink.seasonNumber)
                        putExtra(AndroidNotificationConstant.EXTRA_EPISODE_NUMBER, deepLink.episodeNumber)
                    }
                    is PersonNotificationTarget -> putExtra(AndroidNotificationConstant.EXTRA_PERSON_ID, deepLink.personId)
                    null -> Unit
                }
            }
        val contentPendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        // One group per show/person, not one global group - two different returning series (or two
        // different favorited people) notifying at once should stay as separate stacks, not merge
        // into one. No deepLink at all has nothing to group by, so it posts standalone.
        val groupKey =
            when (deepLink) {
                is EpisodeNotificationTarget -> AndroidNotificationConstant.GROUP_KEY_PREFIX + deepLink.tvShowId
                is PersonNotificationTarget -> AndroidNotificationConstant.PERSON_GROUP_KEY_PREFIX + deepLink.personId
                null -> null
            }
        val groupSummaryTitleRes =
            if (deepLink is PersonNotificationTarget) {
                R.string.notification_person_group_summary_title
            } else {
                R.string.notification_group_summary_title
            }
        val posterBitmap = posterUrl?.let { url -> decodePosterBitmap(url) }
        val notification =
            NotificationCompat
                .Builder(context, AndroidNotificationConstant.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .apply {
                    if (posterBitmap != null) {
                        setLargeIcon(posterBitmap)
                        setStyle(NotificationCompat.BigPictureStyle().bigPicture(posterBitmap).bigLargeIcon(null as Bitmap?))
                    } else {
                        setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    }
                }.setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .apply { if (groupKey != null) setGroup(groupKey) }
                .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        if (groupKey != null) postGroupSummary(context, groupKey, groupSummaryTitleRes)
    }

    // Best-effort: a download/decode failure returns null so the caller falls back to a text-only
    // notification (BigTextStyle) rather than losing the notification entirely - see
    // NotificationImageFetcher's kdoc.
    private suspend fun decodePosterBitmap(url: String): Bitmap? {
        val bytes = NotificationImageFetcher.fetchBytes(url) ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // Required by Android's notification-grouping contract (API 24+; a no-op shim below that on
    // NotificationCompat) - a bare setGroup() on the individual notifications isn't enough on its
    // own to make the system actually stack them, there must also be one summary notification
    // sharing the same group key. Re-posting this on every call is harmless: same notification id
    // per show/person (derived from groupKey, not from the per-reason notificationId) just updates it.
    private fun postGroupSummary(
        context: Context,
        groupKey: String,
        titleRes: Int,
    ) {
        val summary =
            NotificationCompat
                .Builder(context, AndroidNotificationConstant.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(titleRes))
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(groupKey.hashCode(), summary)
    }
}
