package com.ajinkyabadve.kmmmywatchlist.core.notification

/** Records every post instead of showing it. */
class FakeNotifier : Notifier {
    val postedTitles = mutableListOf<String>()

    override suspend fun post(
        notificationId: Int,
        title: String,
        body: String,
        deepLink: NotificationTarget?,
        posterUrl: String?,
    ) {
        postedTitles += title
    }
}
