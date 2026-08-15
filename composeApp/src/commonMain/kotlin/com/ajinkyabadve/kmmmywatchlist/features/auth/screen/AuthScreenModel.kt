package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.lifecycle.ViewModel
import com.ajinkyabadve.kmmmywatchlist.core.UiText
import com.ajinkyabadve.kmmmywatchlist.core.auth.WebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthConstant
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.auth_authorizing
import mywatchlist.composeapp.generated.resources.auth_completing
import mywatchlist.composeapp.generated.resources.auth_error_cancelled
import mywatchlist.composeapp.generated.resources.auth_error_expired
import mywatchlist.composeapp.generated.resources.error_network

sealed interface AuthUiState {
    data object LoggedOut : AuthUiState

    data class Authorizing(
        val statusText: UiText,
    ) : AuthUiState

    data class LoggedIn(
        val session: UserSession,
    ) : AuthUiState

    data class Error(
        val message: UiText,
    ) : AuthUiState
}

/** The `viewModel(key = ...)` string every call site must pass to intentionally share one instance. */
object AuthScreenModelDefaults {
    const val SHARED_KEY = "auth_screen_model_shared"
}

/**
 * Drives the TMDB login/logout flow. Shared by every screen that can start or show that flow (the
 * "My Fav" tab, the top bar's Account screen): both call `viewModel(key = AuthScreenModelDefaults.
 * SHARED_KEY) { AuthScreenModel(authRepository) }` against the same app-wide ViewModelStore (see
 * App.kt's `appViewModelStoreOwner`), so they resolve to the same live instance and logging in/out
 * from either screen is instantly reflected in the other with no extra wiring. The explicit shared
 * key is deliberate rather than relying on `viewModel()`'s default per-class key, so the sharing
 * survives a rename/refactor instead of silently splitting into two instances.
 */
class AuthScreenModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
) : ViewModel() {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.LoggedOut)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collect { session ->
                if (session != null) {
                    _uiState.value = AuthUiState.LoggedIn(session)
                } else {
                    _uiState.value = AuthUiState.LoggedOut
                }
            }
        }
    }

    fun onLoginClicked(webAuthLauncher: WebAuthLauncher) {
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.value = AuthUiState.Authorizing(UiText.Resource(Res.string.auth_authorizing))
            try {
                val requestToken = authRepository.createRequestToken()
                val authUrl = "${AuthConstant.TMDB_AUTH_BASE_URL}$requestToken?redirect_to=${AuthConstant.AUTH_CALLBACK_URL}"
                webAuthLauncher.launchAuth(
                    authUrl = authUrl,
                    redirectScheme = AuthConstant.AUTH_CALLBACK_SCHEME,
                ) { token, approved ->
                    val returnedToken = token ?: requestToken
                    if (approved && returnedToken.isNotEmpty()) {
                        completeSessionCreation(returnedToken)
                    } else {
                        _uiState.value = AuthUiState.Error(UiText.Resource(Res.string.auth_error_cancelled))
                    }
                }
            } catch (e: HttpExceptions) {
                Napier.e(tag = TAG, throwable = e) { "Http error initiating login" }
                _uiState.value = AuthUiState.Error(UiText.Plain(e.message))
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "Network error initiating login" }
                _uiState.value = AuthUiState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: SerializationException) {
                Napier.e(tag = TAG, throwable = e) { "Serialization error initiating login" }
                _uiState.value = AuthUiState.Error(UiText.Resource(Res.string.auth_error_expired))
            } catch (e: ContentConvertException) {
                Napier.e(tag = TAG, throwable = e) { "Content convert error initiating login" }
                _uiState.value = AuthUiState.Error(UiText.Resource(Res.string.auth_error_expired))
            }
        }
    }

    fun checkForPendingWebAuth(webAuthLauncher: WebAuthLauncher) {
        webAuthLauncher.checkPendingAuth { token, approved ->
            if (approved && token.isNotEmpty()) {
                completeSessionCreation(token)
            } else {
                _uiState.value = AuthUiState.Error(UiText.Resource(Res.string.auth_error_cancelled))
            }
        }
    }

    fun completeSessionCreation(requestToken: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _uiState.value = AuthUiState.Authorizing(UiText.Resource(Res.string.auth_completing))
            try {
                val session = authRepository.createSession(requestToken)
                _uiState.value = AuthUiState.LoggedIn(session)
            } catch (e: HttpExceptions) {
                Napier.e(tag = TAG, throwable = e) { "Http error completing session" }
                val errorText =
                    if (e.response.status.value == 401) {
                        UiText.Resource(Res.string.auth_error_expired)
                    } else {
                        UiText.Plain(e.message)
                    }
                _uiState.value = AuthUiState.Error(errorText)
            } catch (e: IOException) {
                Napier.e(tag = TAG, throwable = e) { "Network error completing session" }
                _uiState.value = AuthUiState.Error(UiText.Resource(Res.string.error_network))
            } catch (e: SerializationException) {
                Napier.e(tag = TAG, throwable = e) { "Serialization error completing session" }
                _uiState.value = AuthUiState.Error(UiText.Resource(Res.string.auth_error_expired))
            } catch (e: ContentConvertException) {
                Napier.e(tag = TAG, throwable = e) { "Content convert error completing session" }
                _uiState.value = AuthUiState.Error(UiText.Resource(Res.string.auth_error_expired))
            }
        }
    }

    fun onLogoutClicked() {
        authRepository.clearSession()
        _uiState.value = AuthUiState.LoggedOut
    }

    fun onRetryClicked(webAuthLauncher: WebAuthLauncher) {
        onLoginClicked(webAuthLauncher)
    }

    private companion object {
        const val TAG = "AuthScreenModel"
    }
}
