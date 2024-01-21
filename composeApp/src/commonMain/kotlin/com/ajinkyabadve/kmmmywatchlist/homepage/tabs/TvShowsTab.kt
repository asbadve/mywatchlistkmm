package com.ajinkyabadve.kmmmywatchlist.homepage.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
import com.ajinkyabadve.kmmmywatchlist.homepage.screens.MoviesScreen

object TvShowsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Home)

            return remember {
                TabOptions(
                    index = 0u,
                    title = "Home",
                    icon = icon
                )
            }

        }

    @Composable
    override fun Content() {
        val tabTitle = MoviesTab.options.title
        LifecycleEffect(
            onStarted = { print("Navigator" + "Start tab $tabTitle") },
            onDisposed = { print("Navigator" + "Dispose tab $tabTitle") }
        )

        Navigator(screen = MoviesScreen("popular")) {
            SlideTransition(it) { screen ->
                screen.Content()
            }
        }
    }
}
