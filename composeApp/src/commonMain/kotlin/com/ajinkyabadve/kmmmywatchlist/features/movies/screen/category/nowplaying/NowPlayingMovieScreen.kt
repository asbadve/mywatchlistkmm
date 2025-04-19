package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.nowplaying

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.core.lifecycle.LifecycleEffect
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant.NOW_PLAYING_API_PATH
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.MovieListScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.screenContent
import io.github.aakira.napier.log

@ExperimentalMaterial3WindowSizeClassApi
object NowPlayingMovieScreen : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val icon = rememberVectorPainter(Icons.Default.Share)

            return remember {
                TabOptions(
                    index = 20u,
                    title = "now playing",
                    icon = icon,
                )
            }
        }

    @Composable
    override fun Content() {
        LifecycleEffect(
            onStarted = { log { "onStarted" } },
            onDisposed = { log { "onDisposed" } },
        )
        screenContent(getViewModel(NOW_PLAYING_API_PATH))
    }

    @Composable
    private fun getViewModel(movieFetchType: String): MovieListScreenModel {
        return rememberScreenModel(movieFetchType, factory = {
            MovieListScreenModel(movieFetchType = movieFetchType)
        })
    }
}
