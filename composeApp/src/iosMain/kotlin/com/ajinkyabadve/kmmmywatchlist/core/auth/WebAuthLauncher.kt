package com.ajinkyabadve.kmmmywatchlist.core.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionCompletionHandler
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject

class IosWebAuthLauncher : WebAuthLauncher {
    private var authSession: ASWebAuthenticationSession? = null

    override fun launchAuth(
        authUrl: String,
        redirectScheme: String,
        onResult: (requestToken: String?, approved: Boolean) -> Unit,
    ) {
        val url =
            NSURL.URLWithString(authUrl) ?: run {
                onResult(null, false)
                return
            }

        val completionHandler: ASWebAuthenticationSessionCompletionHandler = { callbackURL, error ->
            if (error != null || callbackURL == null) {
                onResult(null, false)
            } else {
                val components = NSURLComponents.componentsWithURL(callbackURL, resolvingAgainstBaseURL = false)

                @Suppress("UNCHECKED_CAST")
                val queryItems = components?.queryItems as? List<NSURLQueryItem>
                val requestToken = queryItems?.firstOrNull { it.name == "request_token" }?.value
                val approvedStr = queryItems?.firstOrNull { it.name == "approved" }?.value
                // Fail closed: TMDB only appends approved=true on a genuine approval redirect.
                // Treating a missing/unexpected value as approved would let an unapproved token
                // reach createSession(), which TMDB rejects with 401 - surfacing as a confusing
                // "expired" error instead of "cancelled".
                val approved = approvedStr == "true"
                onResult(requestToken, approved)
            }
        }

        authSession =
            ASWebAuthenticationSession(
                uRL = url,
                callbackURLScheme = redirectScheme,
                completionHandler = completionHandler,
            )

        authSession?.presentationContextProvider =
            object : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
                override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): UIWindow =
                    UIApplication.sharedApplication.keyWindow ?: UIWindow()
            }

        authSession?.start()
    }
}

@Composable
actual fun rememberWebAuthLauncher(): WebAuthLauncher = remember { IosWebAuthLauncher() }
