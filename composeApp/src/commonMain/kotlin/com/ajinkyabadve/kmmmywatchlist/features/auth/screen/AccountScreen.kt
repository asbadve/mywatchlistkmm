package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import com.ajinkyabadve.kmmmywatchlist.core.constant.PrivacyConsentConstant
import com.ajinkyabadve.kmmmywatchlist.core.file.BackupFileLauncher
import com.ajinkyabadve.kmmmywatchlist.core.file.rememberBackupFileLauncher
import com.ajinkyabadve.kmmmywatchlist.core.notification.rememberNotificationPermissionRequester
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.AuthErrorContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.AuthorizingContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.LoggedOutContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.UserAvatar
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.auth.viewmodel.AccountScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.backup.screen.BackupRestoreDialogs
import com.ajinkyabadve.kmmmywatchlist.features.backup.viewmodel.BackupScreenModel
import com.ajinkyabadve.kmmmywatchlist.openUrl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.account_screen_title
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.auth_account_welcome
import mywatchlist.composeapp.generated.resources.back_content_description
import mywatchlist.composeapp.generated.resources.fallback_region_picker_title
import mywatchlist.composeapp.generated.resources.region_picker_title
import org.jetbrains.compose.resources.stringResource

private object AccountScreenConstant {
    val PROFILE_AVATAR_SIZE = 72.dp
    val AVATAR_RING_WIDTH = 3.dp
    val DIALOG_WIDTH = 360.dp
    val DIALOG_CORNER_RADIUS = 24.dp
    val DIALOG_MAX_HEIGHT = 560.dp
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
 * state machine behind both entry points - see its kdoc for how the two stay in sync.
 *
 * The settings below the profile belong to [AccountScreenModel] - including which picker is open,
 * which rows the platform shows, and the notification-permission decision. This composable only
 * renders its state and forwards clicks. The one thing it must still create is the permission
 * requester, which registers a launcher with the composition on Android.
 */
@Composable
fun AccountScreen(
    isDialogPresentation: Boolean,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    webAuthLauncher: WebAuthLauncher = rememberWebAuthLauncher(),
    authRepository: AuthRepository = AuthRepositoryImpl(),
    backupFileLauncher: BackupFileLauncher = rememberBackupFileLauncher(),
    backupScreenModel: BackupScreenModel = viewModel { BackupScreenModel() },
    accountScreenModel: AccountScreenModel = viewModel { AccountScreenModel() },
    screenModel: AuthScreenModel =
        viewModel(key = AuthScreenModelDefaults.SHARED_KEY) { AuthScreenModel(authRepository) },
) {
    val uiState by screenModel.uiState.collectAsState()
    val settingsState by accountScreenModel.uiState.collectAsState()
    val notificationPermissionRequester = rememberNotificationPermissionRequester()

    LaunchedEffect(webAuthLauncher) {
        screenModel.checkForPendingWebAuth(webAuthLauncher)
    }

    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            AccountScreenHeader(isDialogPresentation = isDialogPresentation, onCloseClicked = onBackClicked)

            // Scrollable so the settings rows stay reachable once they outgrow one screenful (or
            // the dialog presentation's capped height) - the header above stays pinned.
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
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

                // Region/fallback-region/restricted-mode apply to TMDB's API-key-only endpoints
                // and don't need a session, so they stay reachable no matter the login state - only
                // "Log out" (which needs a session to end) is gated behind LoggedIn.
                SettingsList(
                    uiState = settingsState,
                    onRegionClicked = accountScreenModel::onRegionRowClicked,
                    onFallbackRegionClicked = accountScreenModel::onFallbackRegionRowClicked,
                    onRestrictedModeChanged = accountScreenModel::onRestrictedModeChanged,
                    onTrendingAutoScrollChanged = accountScreenModel::onTrendingAutoScrollChanged,
                    onEpisodeNotificationsChanged = { enabled ->
                        accountScreenModel.onEpisodeNotificationsChanged(enabled, notificationPermissionRequester)
                    },
                    onReleaseRemindersChanged = { enabled ->
                        accountScreenModel.onReleaseRemindersChanged(enabled, notificationPermissionRequester)
                    },
                    onReminderTimeClicked = accountScreenModel::onReminderTimeRowClicked,
                    onBackupClicked = backupScreenModel::open,
                    debugCallbacks =
                        SettingsDebugCallbacks(
                            onPollEpisodeNotificationsClicked = accountScreenModel::onDebugPollEpisodeNotifications,
                            onPollPersonNotificationsClicked = accountScreenModel::onDebugPollPersonNotifications,
                            onPollCollectionNotificationsClicked = accountScreenModel::onDebugPollCollectionNotifications,
                            onResetEpisodeAlertPromptClicked = accountScreenModel::onDebugResetEpisodeAlertPrompt,
                            onFireTestReminderClicked = { accountScreenModel.onDebugFireTestReminder(notificationPermissionRequester) },
                            onPollReleaseRemindersClicked = accountScreenModel::onDebugPollReleaseReminders,
                            onShowPendingRemindersClicked = accountScreenModel::onDebugShowPendingReminders,
                            onClearRemindersClicked = accountScreenModel::onDebugClearReminders,
                        ),
                    onLogoutClicked =
                        if (uiState is AuthUiState.LoggedIn) {
                            {
                                screenModel.onLogoutClicked()
                                onBackClicked()
                            }
                        } else {
                            null
                        },
                    onPrivacyPolicyClicked = { openUrl(PrivacyConsentConstant.PRIVACY_POLICY_URL) },
                )
            }
        }
    }

    if (isDialogPresentation) {
        Card(
            shape = RoundedCornerShape(AccountScreenConstant.DIALOG_CORNER_RADIUS),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            modifier = modifier.width(AccountScreenConstant.DIALOG_WIDTH).heightIn(max = AccountScreenConstant.DIALOG_MAX_HEIGHT),
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

    when (settingsState.openDialog) {
        AccountDialog.REGION ->
            RegionPickerDialog(
                title = stringResource(Res.string.region_picker_title),
                selectedRegionCode = settingsState.regionCode,
                onRegionSelected = accountScreenModel::onRegionSelected,
                onDismiss = accountScreenModel::dismissDialog,
            )

        AccountDialog.FALLBACK_REGION ->
            RegionPickerDialog(
                title = stringResource(Res.string.fallback_region_picker_title),
                selectedRegionCode = settingsState.fallbackRegionCode,
                onRegionSelected = accountScreenModel::onFallbackRegionSelected,
                onDismiss = accountScreenModel::dismissDialog,
            )

        AccountDialog.REMINDER_TIME ->
            ReminderTimePickerDialog(
                initialTime = settingsState.reminderPreference.time,
                is24Hour = settingsState.is24Hour,
                onConfirm = accountScreenModel::onReminderTimeChanged,
                onDismiss = accountScreenModel::dismissDialog,
            )

        null -> Unit
    }

    BackupRestoreDialogs(
        screenModel = backupScreenModel,
        fileLauncher = backupFileLauncher,
        onRestored = accountScreenModel::reload,
    )

    settingsState.pendingReminders?.let { pending ->
        PendingRemindersDialog(pending = pending, onDismiss = accountScreenModel::dismissPendingReminders)
    }

    settingsState.testReminderMessage?.let { message ->
        DebugMessageDialog(message = message, onDismiss = accountScreenModel::dismissTestReminderNotice)
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
