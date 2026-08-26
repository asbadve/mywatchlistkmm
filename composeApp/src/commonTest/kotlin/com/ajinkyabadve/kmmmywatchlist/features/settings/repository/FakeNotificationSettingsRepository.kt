package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

class FakeNotificationSettingsRepository(
    private var episodeNotificationsEnabled: Boolean = false,
) : NotificationSettingsRepository {
    val setEpisodeNotificationsEnabledCalls = mutableListOf<Boolean>()

    override fun isEpisodeNotificationsEnabled(): Boolean = episodeNotificationsEnabled

    override fun setEpisodeNotificationsEnabled(enabled: Boolean) {
        setEpisodeNotificationsEnabledCalls.add(enabled)
        episodeNotificationsEnabled = enabled
    }
}
