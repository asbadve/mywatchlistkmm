package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

class FakePrivacyConsentRepository(
    private var accepted: Boolean = false,
) : PrivacyConsentRepository {
    var markPrivacyPolicyAcceptedCallCount = 0
        private set

    override fun hasAcceptedPrivacyPolicy(): Boolean = accepted

    override fun markPrivacyPolicyAccepted() {
        markPrivacyPolicyAcceptedCallCount++
        accepted = true
    }
}
