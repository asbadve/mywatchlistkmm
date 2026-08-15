package com.ajinkyabadve.kmmmywatchlist.core.ui.auth

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.auth_login_button
import mywatchlist.composeapp.generated.resources.auth_session_expired_message
import mywatchlist.composeapp.generated.resources.auth_session_expired_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionExpiredDialog(
    isVisible: Boolean,
    onSignInClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.auth_session_expired_title))
        },
        text = {
            Text(text = stringResource(Res.string.auth_session_expired_message))
        },
        confirmButton = {
            Button(onClick = onSignInClick) {
                Text(text = stringResource(Res.string.auth_login_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.action_close))
            }
        },
        modifier = modifier.widthIn(max = 400.dp),
    )
}
