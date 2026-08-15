package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.auth_account_welcome
import mywatchlist.composeapp.generated.resources.auth_login_button
import mywatchlist.composeapp.generated.resources.auth_login_message
import mywatchlist.composeapp.generated.resources.auth_login_title
import mywatchlist.composeapp.generated.resources.coming_soon_my_fav_message
import org.jetbrains.compose.resources.stringResource

private object MyFavConstant {
    const val ICON_BOX_SIZE_DP = 64
    const val ICON_SIZE_DP = 36
    const val CARD_ELEVATION_DP = 4
    const val CARD_CORNER_RADIUS_DP = 16
}

@Composable
fun MyFavScreenTab(
    modifier: Modifier = Modifier,
    webAuthLauncher: WebAuthLauncher = rememberWebAuthLauncher(),
    authRepository: AuthRepository = AuthRepositoryImpl(),
    screenModel: MyFavScreenModel = viewModel { MyFavScreenModel(authRepository) },
) {
    val uiState by screenModel.uiState.collectAsState()

    LaunchedEffect(webAuthLauncher) {
        screenModel.checkForPendingWebAuth(webAuthLauncher)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState) {
                is MyFavUiState.LoggedOut -> {
                    LoggedOutContent(
                        onLoginClick = { screenModel.onLoginClicked(webAuthLauncher) },
                    )
                }

                is MyFavUiState.Authorizing -> {
                    AuthorizingContent(statusText = state.statusText.asString())
                }

                is MyFavUiState.LoggedIn -> {
                    LoggedInContent(session = state.session)
                }

                is MyFavUiState.Error -> {
                    ErrorContent(
                        errorMessage = state.message.asString(),
                        onRetryClick = { screenModel.onRetryClicked(webAuthLauncher) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoggedOutContent(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MyFavConstant.CARD_CORNER_RADIUS_DP.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = MyFavConstant.CARD_ELEVATION_DP.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(MyFavConstant.ICON_BOX_SIZE_DP.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(MyFavConstant.ICON_SIZE_DP.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.auth_login_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.auth_login_message),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLoginClick,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.auth_login_button),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AuthorizingContent(
    statusText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = statusText,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun LoggedInContent(
    session: UserSession,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text =
                stringResource(
                    Res.string.auth_account_welcome,
                    session.name.ifEmpty { session.username },
                ),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.coming_soon_my_fav_message),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun ErrorContent(
    errorMessage: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = errorMessage,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetryClick) {
            Text(text = stringResource(Res.string.action_retry))
        }
    }
}
