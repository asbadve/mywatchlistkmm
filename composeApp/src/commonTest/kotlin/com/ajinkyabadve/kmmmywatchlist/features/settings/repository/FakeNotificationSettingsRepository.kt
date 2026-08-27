package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

class FakeNotificationSettingsRepository(
    private var episodeNotificationsEnabled: Boolean = false,
    private var episodeAlertOptInPromptSeen: Boolean = false,
) : NotificationSettingsRepository {
    val setEpisodeNotificationsEnabledCalls = mutableListOf<Boolean>()
    val markEpisodeAlertOptInPromptSeenCallCount get() = markEpisodeAlertOptInPromptSeenCalls

    private var markEpisodeAlertOptInPromptSeenCalls = 0

    override fun isEpisodeNotificationsEnabled(): Boolean = episodeNotificationsEnabled

    override fun setEpisodeNotificationsEnabled(enabled: Boolean) {
        setEpisodeNotificationsEnabledCalls.add(enabled)
        episodeNotificationsEnabled = enabled
    }

    override fun hasSeenEpisodeAlertOptInPrompt(): Boolean = episodeAlertOptInPromptSeen

    override fun markEpisodeAlertOptInPromptSeen() {
        markEpisodeAlertOptInPromptSeenCalls++
        episodeAlertOptInPromptSeen = true
    }

    override fun resetEpisodeAlertOptInPromptForDebug() {
        episodeAlertOptInPromptSeen = false
    }
}
