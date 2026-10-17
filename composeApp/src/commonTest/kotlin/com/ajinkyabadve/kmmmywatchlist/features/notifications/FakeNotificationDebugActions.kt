package com.ajinkyabadve.kmmmywatchlist.features.notifications

/** Counts each debug poll instead of running the real pollers. */
class FakeNotificationDebugActions : NotificationDebugActions {
    var episodePollCount = 0
        private set
    var personPollCount = 0
        private set
    var collectionPollCount = 0
        private set

    override suspend fun pollEpisodeNotificationsNow() {
        episodePollCount++
    }

    override suspend fun pollPersonNotificationsNow() {
        personPollCount++
    }

    override suspend fun pollCollectionNotificationsNow() {
        collectionPollCount++
    }
}
