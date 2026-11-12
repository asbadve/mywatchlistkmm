package com.ajinkyabadve.kmmmywatchlist.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * The [NavEntryDecorator]s every `NavDisplay` entry in the app gets:
 * 1. Saveable state per entry - what `NavDisplay` uses by default.
 * 2. A `ViewModelStore` per entry, so `viewModel(...)` inside a screen gives a ScreenModel that
 *    lives exactly as long as that screen's back-stack entry. It is created on the way in and
 *    cleared when the entry is popped (not on a configuration change).
 *
 * Without (2), every ScreenModel belonged to the Activity: a detail screen reopened later reused
 * the old instance, so state loaded once at creation - a title's favorite/watchlist icons - was
 * never refreshed (KNOWN_BUGS "Detail screen keeps a stale favorite/watchlist state when
 * reopened"), and every detail ScreenModel opened in a session stayed in memory.
 *
 * Switching bottom-nav tabs does not clear the other tabs' ScreenModels: [TopLevelBackStack]
 * keeps every open tab's entries in the flattened back stack and only reorders them. A tab's
 * entries leave - and their ScreenModels are cleared - when the user backs out of it, which the
 * back stack already treats as "start fresh next time".
 *
 * Order matters: the ViewModel decorator needs the saveable-state one before it (its KDoc).
 */
@Composable
fun rememberAppEntryDecorators(): List<NavEntryDecorator<AppKey>> =
    listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    )
