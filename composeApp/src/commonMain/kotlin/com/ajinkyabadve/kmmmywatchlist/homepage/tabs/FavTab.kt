package com.ajinkyabadve.kmmmywatchlist.homepage.tabs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
import com.ajinkyabadve.kmmmywatchlist.features.favorite.screen.FavoriteScreen

object FavTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Home)
            return remember {
                TabOptions(
                    index = 0u,
                    title = "Home",
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
        val tabTitle = options.title
        LifecycleEffect(
            onStarted = { print("NavigatorStart tab $tabTitle") },
            onDisposed = { print("NavigatorDispose tab $tabTitle") },
        )

        Navigator(screen = FavoriteScreen()) {
            SlideTransition(it) { screen ->
                screen.Content()
            }
        }
    }

    data class NavItem(val title: String, val icon: Painter)

    object Tabs {
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185/"
    }
}
