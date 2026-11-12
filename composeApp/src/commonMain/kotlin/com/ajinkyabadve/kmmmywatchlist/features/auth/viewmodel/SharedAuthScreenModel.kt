package com.ajinkyabadve.kmmmywatchlist.features.auth.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AuthScreenModelDefaults
import com.ajinkyabadve.kmmmywatchlist.navigation.appViewModelStoreOwner

/**
 * The one app-wide [AuthScreenModel] - My Fav, the Account screen and every detail screen's action
 * buttons all call this, so logging in or out on one is reflected on the others at once.
 *
 * It lives in the app-session store ([appViewModelStoreOwner]), not the screen's own back-stack
 * entry store: each `NavDisplay` entry has its own store, and a plain
 * `viewModel(key = SHARED_KEY)` would give each screen a separate copy.
 */
@Composable
fun sharedAuthScreenModel(authRepository: AuthRepository): AuthScreenModel =
    viewModel(
        viewModelStoreOwner = appViewModelStoreOwner(),
        key = AuthScreenModelDefaults.SHARED_KEY,
    ) { AuthScreenModel(authRepository) }
