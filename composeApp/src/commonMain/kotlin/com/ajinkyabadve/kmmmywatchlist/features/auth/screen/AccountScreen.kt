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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.core.auth.WebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.auth.rememberWebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.constant.PrivacyConsentConstant
import com.ajinkyabadve.kmmmywatchlist.core.format.toRegionFlagEmoji
import com.ajinkyabadve.kmmmywatchlist.core.notification.NotificationScheduler
import com.ajinkyabadve.kmmmywatchlist.core.notification.rememberNotificationPermissionRequester
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.AuthErrorContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.AuthorizingContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.LoggedOutContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.UserAvatar
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.TrackedMediaRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.CollectionNotificationPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.PersonCreditNotificationPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.TvEpisodeNotificationPoller
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationLedgerRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationReason
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.NotificationSettingsRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.NotificationSettingsRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RegionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RestrictedModeRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.RestrictedModeRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.isDebugBuild
import com.ajinkyabadve.kmmmywatchlist.openUrl
import kotlinx.coroutines.launch
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.account_screen_title
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.auth_account_welcome
import mywatchlist.composeapp.generated.resources.auth_logout
import mywatchlist.composeapp.generated.resources.back_content_description
import mywatchlist.composeapp.generated.resources.debug_poll_collection_notifications_now_description
import mywatchlist.composeapp.generated.resources.debug_poll_collection_notifications_now_label
import mywatchlist.composeapp.generated.resources.debug_poll_notifications_now_description
import mywatchlist.composeapp.generated.resources.debug_poll_notifications_now_label
import mywatchlist.composeapp.generated.resources.debug_poll_person_notifications_now_description
import mywatchlist.composeapp.generated.resources.debug_poll_person_notifications_now_label
import mywatchlist.composeapp.generated.resources.debug_reset_episode_alert_prompt_description
import mywatchlist.composeapp.generated.resources.debug_reset_episode_alert_prompt_label
import mywatchlist.composeapp.generated.resources.fallback_region_picker_title
import mywatchlist.composeapp.generated.resources.region_picker_title
import mywatchlist.composeapp.generated.resources.settings_episode_notifications_description
import mywatchlist.composeapp.generated.resources.settings_episode_notifications_title
import mywatchlist.composeapp.generated.resources.settings_fallback_region_description
import mywatchlist.composeapp.generated.resources.settings_fallback_region_label
import mywatchlist.composeapp.generated.resources.settings_privacy_policy_label
import mywatchlist.composeapp.generated.resources.settings_region_label
import mywatchlist.composeapp.generated.resources.settings_restricted_mode_description
import mywatchlist.composeapp.generated.resources.settings_restricted_mode_label
import mywatchlist.composeapp.generated.resources.tmdb_attribution_notice
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
    regionRepository: RegionRepository = RegionRepositoryImpl(),
    restrictedModeRepository: RestrictedModeRepository = RestrictedModeRepositoryImpl(),
    notificationSettingsRepository: NotificationSettingsRepository = NotificationSettingsRepositoryImpl(),
    screenModel: AuthScreenModel =
        viewModel(key = AuthScreenModelDefaults.SHARED_KEY) { AuthScreenModel(authRepository) },
) {
    val uiState by screenModel.uiState.collectAsState()
    var showRegionPicker by remember { mutableStateOf(false) }
    var showFallbackRegionPicker by remember { mutableStateOf(false) }
    var selectedRegionCode by remember { mutableStateOf(regionRepository.getSelectedRegion()) }
    var fallbackRegionCode by remember { mutableStateOf(regionRepository.getFallbackRegion()) }
    var restrictedModeEnabled by remember { mutableStateOf(restrictedModeRepository.isRestrictedModeEnabled()) }
    var episodeNotificationsEnabled by remember { mutableStateOf(notificationSettingsRepository.isEpisodeNotificationsEnabled()) }
    val notificationPermissionRequester = rememberNotificationPermissionRequester()
    val coroutineScope = rememberCoroutineScope()

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
                    regionCode = selectedRegionCode,
                    fallbackRegionCode = fallbackRegionCode,
                    restrictedModeEnabled = restrictedModeEnabled,
                    episodeNotificationsEnabled = episodeNotificationsEnabled,
                    onRegionClicked = { showRegionPicker = true },
                    onFallbackRegionClicked = { showFallbackRegionPicker = true },
                    onRestrictedModeChanged = { enabled ->
                        restrictedModeRepository.setRestrictedModeEnabled(enabled)
                        restrictedModeEnabled = enabled
                    },
                    onEpisodeNotificationsChanged = { enabled ->
                        if (enabled) {
                            coroutineScope.launch {
                                val granted = notificationPermissionRequester.request()
                                if (granted) {
                                    notificationSettingsRepository.setEpisodeNotificationsEnabled(true)
                                    episodeNotificationsEnabled = true
                                    NotificationScheduler.schedule()
                                }
                            }
                        } else {
                            notificationSettingsRepository.setEpisodeNotificationsEnabled(false)
                            episodeNotificationsEnabled = false
                            NotificationScheduler.cancel()
                        }
                    },
                    showDebugPollNowRow = isDebugBuild(),
                    onDebugPollNowClicked = {
                        coroutineScope.launch {
                            // Debug-only: forces the exact next episode every tracked returning
                            // series currently has to look newly-announced again, so this can be
                            // re-tapped for a fresh notification on demand instead of being blocked
                            // by whatever a previous real poll already recorded. Scoped to episode
                            // reasons only (clearForReasonForDebug), not the whole ledger, so this
                            // never disturbs 3b's separate "Poll person notifications now" row below.
                            val trackedMediaRepository = TrackedMediaRepositoryImpl()
                            val notificationLedgerRepository = NotificationLedgerRepositoryImpl()
                            trackedMediaRepository.resetAllTvPollStateForDebug()
                            notificationLedgerRepository.clearForReasonForDebug(NotificationReason.EPISODE_ANNOUNCED)
                            notificationLedgerRepository.clearForReasonForDebug(NotificationReason.EPISODE_AIRING)
                            TvEpisodeNotificationPoller(
                                trackedMediaRepository = trackedMediaRepository,
                                notificationLedgerRepository = notificationLedgerRepository,
                            ).poll()
                        }
                    },
                    onDebugPollPersonNotificationsNowClicked = {
                        coroutineScope.launch {
                            // Same idea as onDebugPollNowClicked above, but for 3b - kept as its own
                            // row/repository set (requested 2026-08-27) so testing person-credit
                            // notifications never resets or races with TV episode testing.
                            // seedOneNewCreditForDebug (not a blanket credit-state reset) holds back
                            // exactly one of each favorited person's current credits, so the poll()
                            // below notifies once per person, not once per credit (requested
                            // 2026-08-27 - a blanket reset floods anyone with a real filmography).
                            val favoritePersonRepository = FavoritePersonRepositoryImpl()
                            val notificationLedgerRepository = NotificationLedgerRepositoryImpl()
                            notificationLedgerRepository.clearForReasonForDebug(NotificationReason.PERSON_NEW_CREDIT)
                            val poller =
                                PersonCreditNotificationPoller(
                                    favoritePersonRepository = favoritePersonRepository,
                                    notificationLedgerRepository = notificationLedgerRepository,
                                )
                            poller.seedOneNewCreditForDebug()
                            poller.poll()
                        }
                    },
                    onDebugPollCollectionNotificationsNowClicked = {
                        coroutineScope.launch {
                            // Same idea as onDebugPollPersonNotificationsNowClicked above, but for
                            // 3c - own row/repository set so testing collection notifications never
                            // resets or races with episode/person testing.
                            val favoriteCollectionRepository = FavoriteCollectionRepositoryImpl()
                            val notificationLedgerRepository = NotificationLedgerRepositoryImpl()
                            notificationLedgerRepository.clearForReasonForDebug(NotificationReason.COLLECTION_NEW_PART)
                            val poller =
                                CollectionNotificationPoller(
                                    favoriteCollectionRepository = favoriteCollectionRepository,
                                    notificationLedgerRepository = notificationLedgerRepository,
                                )
                            poller.seedOneNewPartForDebug()
                            poller.poll()
                        }
                    },
                    onDebugResetEpisodeAlertPromptClicked = {
                        notificationSettingsRepository.setEpisodeNotificationsEnabled(false)
                        notificationSettingsRepository.resetEpisodeAlertOptInPromptForDebug()
                        episodeNotificationsEnabled = false
                        NotificationScheduler.cancel()
                    },
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

    if (showRegionPicker) {
        RegionPickerDialog(
            title = stringResource(Res.string.region_picker_title),
            selectedRegionCode = selectedRegionCode,
            onRegionSelected = { code ->
                regionRepository.setSelectedRegion(code)
                selectedRegionCode = code
            },
            onDismiss = { showRegionPicker = false },
        )
    }

    if (showFallbackRegionPicker) {
        RegionPickerDialog(
            title = stringResource(Res.string.fallback_region_picker_title),
            selectedRegionCode = fallbackRegionCode,
            onRegionSelected = { code ->
                regionRepository.setFallbackRegion(code)
                fallbackRegionCode = code
            },
            onDismiss = { showFallbackRegionPicker = false },
        )
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

/**
 * "Region", "Default fallback region", "Restricted Mode" and "Log out" - future settings rows
 * will follow the same shape.
 */
@Composable
private fun SettingsList(
    regionCode: String,
    fallbackRegionCode: String,
    restrictedModeEnabled: Boolean,
    episodeNotificationsEnabled: Boolean,
    onRegionClicked: () -> Unit,
    onFallbackRegionClicked: () -> Unit,
    onRestrictedModeChanged: (Boolean) -> Unit,
    onEpisodeNotificationsChanged: (Boolean) -> Unit,
    showDebugPollNowRow: Boolean,
    onDebugPollNowClicked: () -> Unit,
    onDebugPollPersonNotificationsNowClicked: () -> Unit,
    onDebugPollCollectionNotificationsNowClicked: () -> Unit,
    onDebugResetEpisodeAlertPromptClicked: () -> Unit,
    onLogoutClicked: (() -> Unit)?,
    onPrivacyPolicyClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsRow(
            label = stringResource(Res.string.settings_region_label),
            value = "${regionCode.toRegionFlagEmoji()} $regionCode".trim(),
            onClick = onRegionClicked,
        )
        SettingsRow(
            label = stringResource(Res.string.settings_fallback_region_label),
            value = "${fallbackRegionCode.toRegionFlagEmoji()} $fallbackRegionCode".trim(),
            onClick = onFallbackRegionClicked,
            description = stringResource(Res.string.settings_fallback_region_description),
        )
        SettingsSwitchRow(
            label = stringResource(Res.string.settings_restricted_mode_label),
            description = stringResource(Res.string.settings_restricted_mode_description),
            checked = restrictedModeEnabled,
            onCheckedChange = onRestrictedModeChanged,
        )
        SettingsSwitchRow(
            label = stringResource(Res.string.settings_episode_notifications_title),
            description = stringResource(Res.string.settings_episode_notifications_description),
            checked = episodeNotificationsEnabled,
            onCheckedChange = onEpisodeNotificationsChanged,
        )
        if (showDebugPollNowRow) {
            SettingsRow(
                label = stringResource(Res.string.debug_poll_notifications_now_label),
                description = stringResource(Res.string.debug_poll_notifications_now_description),
                onClick = onDebugPollNowClicked,
            )
            SettingsRow(
                label = stringResource(Res.string.debug_poll_person_notifications_now_label),
                description = stringResource(Res.string.debug_poll_person_notifications_now_description),
                onClick = onDebugPollPersonNotificationsNowClicked,
            )
            SettingsRow(
                label = stringResource(Res.string.debug_poll_collection_notifications_now_label),
                description = stringResource(Res.string.debug_poll_collection_notifications_now_description),
                onClick = onDebugPollCollectionNotificationsNowClicked,
            )
            SettingsRow(
                label = stringResource(Res.string.debug_reset_episode_alert_prompt_label),
                description = stringResource(Res.string.debug_reset_episode_alert_prompt_description),
                onClick = onDebugResetEpisodeAlertPromptClicked,
            )
        }
        onLogoutClicked?.let {
            SettingsRow(
                label = stringResource(Res.string.auth_logout),
                onClick = it,
                labelColor = MaterialTheme.colorScheme.error,
            )
        }
        SettingsRow(
            label = stringResource(Res.string.settings_privacy_policy_label),
            onClick = onPrivacyPolicyClicked,
        )
        Text(
            text = stringResource(Res.string.tmdb_attribution_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    onClick: () -> Unit,
    value: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onBackground,
    description: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = labelColor,
            )
            description?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        value?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

/**
 * A boolean settings row - trailing [Switch] instead of [SettingsRow]'s navigating chevron, with
 * an optional second line explaining what the toggle actually does.
 */
@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            description?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
