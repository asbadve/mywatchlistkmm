package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.PrivacyConsentConstant
import com.russhwolf.settings.Settings

interface PrivacyConsentRepository {
    fun hasAcceptedPrivacyPolicy(): Boolean

    fun markPrivacyPolicyAccepted()
}

/**
 * Backs [com.ajinkyabadve.kmmmywatchlist.core.ui.privacy.PrivacyConsentGate] - TMDB's API terms
 * and the app stores both require accepting the privacy policy before the app does anything with
 * account or local data. Persisted the same way every other one-time device flag in this app is
 * (multiplatform-settings, mirrors [NotificationSettingsRepositoryImpl]'s opt-in-prompt-seen
 * pattern) - shown once per install, never again once accepted.
 */
class PrivacyConsentRepositoryImpl(
    private val settings: Settings = com.ajinkyabadve.kmmmywatchlist.createSettings(),
) : PrivacyConsentRepository {
    override fun hasAcceptedPrivacyPolicy(): Boolean = settings.getBoolean(PrivacyConsentConstant.KEY_PRIVACY_POLICY_ACCEPTED, false)

    override fun markPrivacyPolicyAccepted() {
        settings.putBoolean(PrivacyConsentConstant.KEY_PRIVACY_POLICY_ACCEPTED, true)
    }
}
