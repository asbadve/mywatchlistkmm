package com.ajinkyabadve.kmmmywatchlist.core.ui.auth

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession

private object AccountAvatarButtonConstant {
    val AVATAR_SIZE = 32.dp
}

/** The top app bar's entry point into the account screen - just the avatar, no menu of its own. */
@Composable
fun AccountAvatarButton(
    session: UserSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        UserAvatar(
            avatarUrl = session.avatarUrl,
            username = session.username,
            size = AccountAvatarButtonConstant.AVATAR_SIZE,
        )
    }
}
