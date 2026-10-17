package com.ajinkyabadve.kmmmywatchlist.core.notification

/** Answers every permission request with [granted] and counts the prompts. */
class FakeNotificationPermissionRequester(
    private val granted: Boolean = true,
) : NotificationPermissionRequester {
    var requestCount = 0
        private set

    override suspend fun request(): Boolean {
        requestCount++
        return granted
    }
}
