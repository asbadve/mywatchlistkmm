package com.ajinkyabadve.kmmmywatchlist.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * The app-session [ViewModelStoreOwner] App.kt creates, for the few ScreenModels that are
 * deliberately shared across screens (AuthScreenModel).
 *
 * Inside a `NavDisplay` entry, [LocalViewModelStoreOwner] is that entry's own store (see
 * [rememberAppEntryDecorators]), so a plain `viewModel(key = ...)` there gives one instance per
 * screen. Passing this owner explicitly as `viewModel(viewModelStoreOwner = ...)` is the platform's
 * way to reach a wider scope - Navigation 3 has no "shared between entries" decorator for a
 * ScreenModel used by unrelated screens - this local only makes that owner reachable.
 *
 * `null` outside App.kt (tests rendering a single screen); [appViewModelStoreOwner] then falls back
 * to the ambient owner, which is all there is to share in that case.
 */
val LocalAppViewModelStoreOwner: ProvidableCompositionLocal<ViewModelStoreOwner?> =
    compositionLocalOf { null }

/** The app-session owner when provided, otherwise the ambient one. */
@Composable
fun appViewModelStoreOwner(): ViewModelStoreOwner =
    LocalAppViewModelStoreOwner.current
        ?: checkNotNull(LocalViewModelStoreOwner.current) { NO_OWNER_MESSAGE }

private const val NO_OWNER_MESSAGE = "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
