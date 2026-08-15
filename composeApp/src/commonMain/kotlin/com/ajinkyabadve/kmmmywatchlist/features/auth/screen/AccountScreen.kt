package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.core.auth.WebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.auth.rememberWebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.AuthErrorContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.AuthorizingContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.LoggedOutContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.UserAvatar
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.account_screen_title
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.auth_account_welcome
import mywatchlist.composeapp.generated.resources.auth_logout
import mywatchlist.composeapp.generated.resources.back_content_description
import org.jetbrains.compose.resources.stringResource

private object AccountScreenConstant {
    val PROFILE_AVATAR_SIZE = 72.dp
    val AVATAR_RING_WIDTH = 3.dp
    val DIALOG_WIDTH = 360.dp
    val DIALOG_CORNER_RADIUS = 24.dp
}

/**
 * The account destination reached from the top bar's avatar - full-screen (with a back arrow) on
 * compact width, or a centered card (with a close button) inside
 * [androidx.navigation3.scene.DialogSceneStrategy]'s Dialog on expanded width - see the
 * `entry<AccountKey>` wiring in App.kt for which chrome this renders under.
 *
 * Renders every [AuthUiState], not just [AuthUiState.LoggedIn]: the avatar button that opens this
 * screen is shown whether or not the user is signed in, so tapping it while logged out lands on
 * the same sign-in card [com.ajinkyabadve.kmmmywatchlist.features.trending.screen.MyFavScreenTab]
 * uses, and logging in here follows the identical TMDB OAuth flow. [AuthScreenModel] is the shared
 * state machine behind both entry points - see its kdoc for how the two stay in sync. Settings
 * rows beyond "Log out" belong in [SettingsList] as the account grows.
 */
@Composable
fun AccountScreen(
    isDialogPresentation: Boolean,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    webAuthLauncher: WebAuthLauncher = rememberWebAuthLauncher(),
    authRepository: AuthRepository = AuthRepositoryImpl(),
    screenModel: AuthScreenModel =
        viewModel(key = AuthScreenModelDefaults.SHARED_KEY) { AuthScreenModel(authRepository) },
) {
    val uiState by screenModel.uiState.collectAsState()

    LaunchedEffect(webAuthLauncher) {
        screenModel.checkForPendingWebAuth(webAuthLauncher)
    }

    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            AccountScreenHeader(isDialogPresentation = isDialogPresentation, onCloseClicked = onBackClicked)

            when (val state = uiState) {
                is AuthUiState.LoggedOut -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        LoggedOutContent(onLoginClick = { screenModel.onLoginClicked(webAuthLauncher) })
                    }
                }

                is AuthUiState.Authorizing -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AuthorizingContent(statusText = state.statusText.asString())
                    }
                }

                is AuthUiState.LoggedIn -> {
                    ProfileSection(state.session)
                    HorizontalDivider()
                    SettingsList(
                        onLogoutClicked = {
                            screenModel.onLogoutClicked()
                            onBackClicked()
                        },
                    )
                }

                is AuthUiState.Error -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        AuthErrorContent(
                            errorMessage = state.message.asString(),
                            onRetryClick = { screenModel.onRetryClicked(webAuthLauncher) },
                        )
                    }
                }
            }
        }
    }

    if (isDialogPresentation) {
        Card(
            shape = RoundedCornerShape(AccountScreenConstant.DIALOG_CORNER_RADIUS),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            modifier = modifier.width(AccountScreenConstant.DIALOG_WIDTH),
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            content()
        }
    }
}

@Composable
private fun AccountScreenHeader(
    isDialogPresentation: Boolean,
    onCloseClicked: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!isDialogPresentation) {
            IconButton(onClick = onCloseClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.back_content_description),
                )
            }
        }

        Text(
            text = stringResource(Res.string.account_screen_title),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f).padding(start = if (isDialogPresentation) 16.dp else 0.dp),
        )

        if (isDialogPresentation) {
            IconButton(onClick = onCloseClicked) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.action_close),
                )
            }
        }
    }
}

@Composable
private fun ProfileSection(session: UserSession) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(AccountScreenConstant.PROFILE_AVATAR_SIZE)
                    .clip(CircleShape)
                    .border(AccountScreenConstant.AVATAR_RING_WIDTH, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            UserAvatar(
                avatarUrl = session.avatarUrl,
                username = session.username,
                size = AccountScreenConstant.PROFILE_AVATAR_SIZE - AccountScreenConstant.AVATAR_RING_WIDTH * 2,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.auth_account_welcome, session.name.ifEmpty { session.username }),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = "@${session.username}",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

/** Just "Log out" for now - the shape future settings rows (notifications, region, ...) will follow. */
@Composable
private fun SettingsList(onLogoutClicked: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsRow(label = stringResource(Res.string.auth_logout), onClick = onLogoutClicked)
    }
}

@Composable
private fun SettingsRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}
