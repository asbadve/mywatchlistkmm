package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.RestrictedModeConstant
import com.russhwolf.settings.Settings

interface RestrictedModeRepository {
    fun isRestrictedModeEnabled(): Boolean

    fun setRestrictedModeEnabled(enabled: Boolean)
}

/** Device-local adult-content gate for `include_adult` search calls - on (restricted) by default. */
class RestrictedModeRepositoryImpl(
    private val settings: Settings = com.ajinkyabadve.kmmmywatchlist.createSettings(),
) : RestrictedModeRepository {
    override fun isRestrictedModeEnabled(): Boolean = settings.getBoolean(RestrictedModeConstant.KEY_RESTRICTED_MODE_ENABLED, true)

    override fun setRestrictedModeEnabled(enabled: Boolean) {
        settings.putBoolean(RestrictedModeConstant.KEY_RESTRICTED_MODE_ENABLED, enabled)
    }
}
