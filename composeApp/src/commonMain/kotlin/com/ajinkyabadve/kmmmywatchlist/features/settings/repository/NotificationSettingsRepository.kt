package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.NotificationSettingsConstant
import com.russhwolf.settings.Settings

interface NotificationSettingsRepository {
    fun isEpisodeNotificationsEnabled(): Boolean

    fun setEpisodeNotificationsEnabled(enabled: Boolean)
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
}
