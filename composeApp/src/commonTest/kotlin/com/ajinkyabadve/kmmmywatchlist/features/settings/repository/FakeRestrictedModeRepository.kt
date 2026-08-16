package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

class FakeRestrictedModeRepository(
    private var restrictedModeEnabled: Boolean = true,
) : RestrictedModeRepository {
    val setRestrictedModeCalls = mutableListOf<Boolean>()

    override fun isRestrictedModeEnabled(): Boolean = restrictedModeEnabled

    override fun setRestrictedModeEnabled(enabled: Boolean) {
        setRestrictedModeCalls.add(enabled)
        restrictedModeEnabled = enabled
    }
}
