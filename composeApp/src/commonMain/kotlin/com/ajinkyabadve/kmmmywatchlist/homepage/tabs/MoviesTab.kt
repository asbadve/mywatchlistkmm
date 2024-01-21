package com.ajinkyabadve.kmmmywatchlist.homepage.tabs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.transitions.SlideTransition
import com.ajinkyabadve.kmmmywatchlist.design.MovieCard
import com.ajinkyabadve.kmmmywatchlist.homepage.screens.MoviesScreen
import com.seiko.imageloader.rememberImagePainter


object MoviesTab : Tab {

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
        val tabTitle = options.title
        LifecycleEffect(
            onStarted = { print("Navigator" + "Start tab $tabTitle") },
            onDisposed = { print("Navigator" + "Dispose tab $tabTitle") }
        )

        Navigator(screen = MoviesScreen("now_playing")) {
            SlideTransition(it) { screen ->
                screen.Content()
            }
        }
    }

    fun getGridColumn(windowSizeClass: WindowSizeClass): Int {
        return when (WindowSize.basedOnWindowSizeClass(windowSizeClass.widthSizeClass.toString())) {
            WindowSize.COMPACT -> {
                2
            }

            WindowSize.EXPANDED -> {
                6
            }

            WindowSize.MEDIUM -> {
                3
            }
        }
    }

    enum class WindowSize {
        COMPACT,
        MEDIUM,
        EXPANDED,
        ;

        // Factory method that creates an instance of the class based on window width
        companion object {

            fun basedOnWindowSizeClass(windowWidthType: String): WindowSize {
                return when (windowWidthType) {
                    "WindowWidthSizeClass.Compact" -> COMPACT
                    "WindowWidthSizeClass.Medium" -> MEDIUM
                    else -> EXPANDED
                }
            }

            fun basedOnWidth(windowWidth: Dp): WindowSize {
                return when {
                    windowWidth < 600.dp -> COMPACT
                    windowWidth < 840.dp -> MEDIUM
                    else -> EXPANDED
                }
            }
        }
    }


    @Composable
    fun MovieRow(imageUrl: String?, title: String) {
        var painter: Painter? = null

        imageUrl?.let {
            painter =
                rememberImagePainter(url = imageUrl, filterQuality = FilterQuality.Medium)
        }

        Row(Modifier.padding(8.dp)) {
            painter?.let {
                MovieCard(
                    Modifier,
                    title,
                    it,
                )
            }
        }
    }

    data class NavItem(val title: String, val icon: Painter)


    object Tabs {
        const val MOVIES = "Movies"
        const val NOW_PLAYING_MOVIES = "Now Playing"
        const val UPCOMING_MOVIES = "Upcoming"
        const val POPULAR_MOVIES = "Popular"
        const val TOP_RATED_MOVIES = "Top Rated"
        const val TV_SHOWS = "Tv shows"
        const val PERSON = "Person"
        const val DISCOVER = "Discover"
        const val FAV = "My Fav"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w185/"
    }


}