package com.ajinkyabadve.kmmmywatchlist.core.notification

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNumber
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

// A plain `object : NSObject(), SomeProtocol` singleton fails Kotlin/Native's link step ("should
// have been lowered" in CodeGenerator.typeInfoForAllocation) - confirmed 2026-08-27 building
// against Kotlin/Native's current compiler. The known workaround is a private class instantiated
// lazily instead, wrapped in a plain (non-NSObject) Kotlin object for the public API.
@OptIn(ExperimentalForeignApi::class)
private class NotificationTapDelegateImpl :
    NSObject(),
    UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
        val tvShowId = (userInfo[IosNotificationUserInfoKeyConstant.TV_SHOW_ID] as? NSNumber)?.longLongValue
        val seasonNumber = (userInfo[IosNotificationUserInfoKeyConstant.SEASON_NUMBER] as? NSNumber)?.intValue
        val episodeNumber = (userInfo[IosNotificationUserInfoKeyConstant.EPISODE_NUMBER] as? NSNumber)?.intValue
        val personId = (userInfo[IosNotificationUserInfoKeyConstant.PERSON_ID] as? NSNumber)?.longLongValue
        if (tvShowId != null && seasonNumber != null && episodeNumber != null) {
            PendingNotificationTarget.set(EpisodeNotificationTarget(tvShowId, seasonNumber, episodeNumber))
        } else if (personId != null) {
            PendingNotificationTarget.set(PersonNotificationTarget(personId))
        }
        withCompletionHandler()
    }

    // Without this, iOS silently swallows a notification delivered while the app is already in
    // the foreground (unlike Android, which always shows it) - confirmed 2026-08-26: notifications
    // were being added to the notification center with no error, just never visibly presented
    // while testing from the Account screen's debug row (the app is, by definition, foregrounded
    // for that). Banner+sound matches what the notification would look like backgrounded.
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionSound)
    }
}

/**
 * Handles a tap on a delivered notification - reads whichever [NotificationTarget] shape
 * [LocalNotifier.post] embedded in the notification's `userInfo` and surfaces it through
 * [PendingNotificationTarget], the same observable `App.kt`'s `MainAppScreen` already watches for
 * Android's equivalent `PendingIntent`-extras path. Registered once, in `MainViewController()`
 * (`Main.kt`) - the earliest point in this app's iOS launch sequence, so a cold launch from a
 * notification tap is caught, not just a tap while already running.
 */
object NotificationTapDelegate {
    private val delegate = NotificationTapDelegateImpl()

    fun register() {
        UNUserNotificationCenter.currentNotificationCenter().delegate = delegate
    }
}
