package com.ajinkyabadve.kmmmywatchlist.core.auth

/** Approves every login attempt immediately - shared by every UI test that drives a login flow. */
class FakeWebAuthLauncher : WebAuthLauncher {
    override fun launchAuth(
        authUrl: String,
        redirectScheme: String,
        onResult: (requestToken: String?, approved: Boolean) -> Unit,
    ) {
        onResult("fake_token", true)
    }
}
