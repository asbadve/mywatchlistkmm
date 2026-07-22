package com.ajinkyabadve.kmmmywatchlist.core

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * User-visible text that either comes from a string resource (preferred, localizable) or from
 * dynamic data such as a server-provided error message. Lets non-composable layers (screen
 * models) reference localized text without resolving it themselves.
 */
sealed interface UiText {
    data class Plain(
        val value: String,
    ) : UiText

    data class Resource(
        val res: StringResource,
    ) : UiText
}

@Composable
fun UiText.asString(): String =
    when (this) {
        is UiText.Plain -> value
        is UiText.Resource -> stringResource(res)
    }
