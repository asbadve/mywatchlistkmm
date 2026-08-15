package com.ajinkyabadve.kmmmywatchlist.core.ui.auth

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.account_screen_title
import org.jetbrains.compose.resources.stringResource

private object AccountAvatarButtonConstant {
    val AVATAR_SIZE = 32.dp
}

/**
 * The top app bar's entry point into the account screen - always shown, logged in or out, so
 * signing in is reachable from anywhere rather than only from the "My Fav" tab. Signed out,
 * [session] is null and [UserAvatar] falls back to its guest icon on its own.
 */
@Composable
fun AccountAvatarButton(
    session: UserSession?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        UserAvatar(
            avatarUrl = session?.avatarUrl,
            username = session?.username ?: stringResource(Res.string.account_screen_title),
            size = AccountAvatarButtonConstant.AVATAR_SIZE,
        )
    }
}
