package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.core.auth.WebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.auth.rememberWebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.AuthErrorContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.AuthorizingContent
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.LoggedOutContent
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.AccountMediaRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.repository.ListsRepository
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.MyFavTabs
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthScreenModelDefaults
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthUiState

@Composable
fun MyFavScreenTab(
    modifier: Modifier = Modifier,
    onMovieSelected: (movieId: Long) -> Unit = {},
    onTvSelected: (tvId: Long) -> Unit = {},
    onListSelected: (listId: Long) -> Unit = {},
    webAuthLauncher: WebAuthLauncher = rememberWebAuthLauncher(),
    authRepository: AuthRepository = AuthRepositoryImpl(),
    screenModel: AuthScreenModel =
        viewModel(key = AuthScreenModelDefaults.SHARED_KEY) { AuthScreenModel(authRepository) },
    // Test-only seams, same pattern MovieScreenTabs uses for its per-tab repositories.
    accountMediaRepository: AccountMediaRepository? = null,
    listsRepository: ListsRepository? = null,
) {
    val uiState by screenModel.uiState.collectAsState()

    LaunchedEffect(webAuthLauncher) {
        screenModel.checkForPendingWebAuth(webAuthLauncher)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (val state = uiState) {
            is AuthUiState.LoggedOut -> {
                CenteredCardContent {
                    LoggedOutContent(onLoginClick = { screenModel.onLoginClicked(webAuthLauncher) })
                }
            }

            is AuthUiState.Authorizing -> {
                CenteredCardContent {
                    AuthorizingContent(statusText = state.statusText.asString())
                }
            }

            is AuthUiState.LoggedIn -> {
                MyFavTabs(
                    session = state.session,
                    onMovieSelected = onMovieSelected,
                    onTvSelected = onTvSelected,
                    onListSelected = onListSelected,
                    accountMediaRepository = accountMediaRepository,
                    listsRepository = listsRepository,
                )
            }

            is AuthUiState.Error -> {
                CenteredCardContent {
                    AuthErrorContent(
                        errorMessage = state.message.asString(),
                        onRetryClick = { screenModel.onRetryClicked(webAuthLauncher) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredCardContent(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
