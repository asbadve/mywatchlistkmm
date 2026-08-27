package com.ajinkyabadve.kmmmywatchlist.core.notification

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAttachment
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

// userInfo key names - read back by NotificationTapDelegate.kt when a notification is tapped.
internal object IosNotificationUserInfoKeyConstant {
    const val TV_SHOW_ID = "tvShowId"
    const val SEASON_NUMBER = "seasonNumber"
    const val EPISODE_NUMBER = "episodeNumber"
    const val PERSON_ID = "personId"
}

private const val LOCAL_NOTIFIER_TAG = "LocalNotifierIos"

@OptIn(ExperimentalForeignApi::class)
actual object LocalNotifier {
    actual suspend fun post(
        notificationId: Int,
        title: String,
        body: String,
        deepLink: NotificationTarget?,
        posterUrl: String?,
    ) {
        val content =
            UNMutableNotificationContent().apply {
                setTitle(title)
                setBody(body)
                setSound(UNNotificationSound.defaultSound)
                // No deepLink means no userInfo at all - NotificationTapDelegate simply has
                // nothing to navigate to.
                when (deepLink) {
                    is EpisodeNotificationTarget ->
                        setUserInfo(
                            mapOf(
                                IosNotificationUserInfoKeyConstant.TV_SHOW_ID to deepLink.tvShowId,
                                IosNotificationUserInfoKeyConstant.SEASON_NUMBER to deepLink.seasonNumber,
                                IosNotificationUserInfoKeyConstant.EPISODE_NUMBER to deepLink.episodeNumber,
                            ),
                        )
                    is PersonNotificationTarget ->
                        setUserInfo(mapOf(IosNotificationUserInfoKeyConstant.PERSON_ID to deepLink.personId))
                    null -> Unit
                }
                posterAttachment(posterUrl)?.let { setAttachments(listOf(it)) }
            }
        val request =
            UNNotificationRequest.requestWithIdentifier(
                identifier = notificationId.toString(),
                content = content,
                trigger = null,
            )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
    }

    // Best-effort, same reasoning as the Android actual's decodePosterBitmap: any failure (download,
    // temp-file write, or UNNotificationAttachment construction) returns null so the notification
    // still posts without an image rather than not posting at all.
    private suspend fun posterAttachment(posterUrl: String?): UNNotificationAttachment? {
        if (posterUrl == null) return null
        val bytes = NotificationImageFetcher.fetchBytes(posterUrl) ?: return null
        val data = bytes.toNSData()
        val filePath = NSTemporaryDirectory() + NSUUID().UUIDString() + ".jpg"
        if (!NSFileManager.defaultManager.createFileAtPath(filePath, data, null)) return null
        val fileURL = NSURL.fileURLWithPath(filePath)
        return memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            val attachment = UNNotificationAttachment.attachmentWithIdentifier("poster", fileURL, null, errorVar.ptr)
            if (errorVar.value != null) {
                Napier.e(tag = LOCAL_NOTIFIER_TAG) { "Failed to create notification attachment: ${errorVar.value}" }
                null
            } else {
                attachment
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
