package com.ajinkyabadve.kmmmywatchlist.core.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage

private object UserAvatarConstant {
    const val FALLBACK_ICON_SIZE_FRACTION = 0.75f
}

@Composable
fun UserAvatar(
    avatarUrl: String?,
    username: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    if (!avatarUrl.isNullOrEmpty()) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = username,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
    } else {
        Box(
            modifier =
                modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = username,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(size * UserAvatarConstant.FALLBACK_ICON_SIZE_FRACTION),
            )
        }
    }
}
