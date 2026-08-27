package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.NotificationSettingsConstant
import com.russhwolf.settings.Settings

interface NotificationSettingsRepository {
    fun isEpisodeNotificationsEnabled(): Boolean

    fun setEpisodeNotificationsEnabled(enabled: Boolean)

    /** True once the in-context "turn on episode alerts?" prompt (shown on first favorite/watchlist
     *  of a TV show) has been shown and actioned once - it never interrupts the user a second time,
     *  whether they accepted or dismissed it. */
    fun hasSeenEpisodeAlertOptInPrompt(): Boolean

    fun markEpisodeAlertOptInPromptSeen()

    /** Debug builds only: clears the "seen" flag so the opt-in prompt can be re-tested without
     *  reinstalling - mirrors `TrackedMediaRepository.resetAllTvPollStateForDebug`'s pattern. */
    fun resetEpisodeAlertOptInPromptForDebug()
}

/** Device-local opt-in for checklist item 3a's returning-series episode alerts - off by default,
 *  since turning it on requires an OS permission grant. */
class NotificationSettingsRepositoryImpl(
    private val settings: Settings = com.ajinkyabadve.kmmmywatchlist.createSettings(),
) : NotificationSettingsRepository {
    override fun isEpisodeNotificationsEnabled(): Boolean =
        settings.getBoolean(NotificationSettingsConstant.KEY_EPISODE_NOTIFICATIONS_ENABLED, false)

    override fun setEpisodeNotificationsEnabled(enabled: Boolean) {
        settings.putBoolean(NotificationSettingsConstant.KEY_EPISODE_NOTIFICATIONS_ENABLED, enabled)
    }

    override fun hasSeenEpisodeAlertOptInPrompt(): Boolean =
        settings.getBoolean(NotificationSettingsConstant.KEY_EPISODE_ALERT_OPT_IN_PROMPT_SEEN, false)

    override fun markEpisodeAlertOptInPromptSeen() {
        settings.putBoolean(NotificationSettingsConstant.KEY_EPISODE_ALERT_OPT_IN_PROMPT_SEEN, true)
    }

    override fun resetEpisodeAlertOptInPromptForDebug() {
        settings.putBoolean(NotificationSettingsConstant.KEY_EPISODE_ALERT_OPT_IN_PROMPT_SEEN, false)
    }
}
