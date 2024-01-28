@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.ajinkyabadve.kmmmywatchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabDisposable
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.design.searchbox.SearchBox
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TvShowsTab
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_surface
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@ExperimentalMaterial3Api
@Composable
internal fun App() = AppTheme {
    val windowSize = getWindowSize(calculateWindowSizeClass())
    TabNavigator(MoviesTab, tabDisposable = {
        TabDisposable(
            navigator = it, tabs = listOf(MoviesTab, TvShowsTab)
        )
    }) { tabNavigator ->
        Scaffold(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars), topBar = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (windowSize == WindowSize.COMPACT) {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = 0.dp, 16.dp, 16.dp, 16.dp)
                            ) {
                                SearchBox(
                                    modifier = Modifier.fillMaxWidth(),
                                    hint = "Search for Movies & Tv shows",
                                    onClick = {},
                                )
                            }
                        },
                    )
                }
            }
        }, bottomBar = {
            if (windowSize == WindowSize.COMPACT || windowSize == WindowSize.MEDIUM) {
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painterResource("baseline_movie_24.xml"),
                                contentDescription = MoviesTab.Tabs.MOVIES,
                            )
                        },
                        label = { Text(MoviesTab.Tabs.MOVIES) },
                        selected = tabNavigator.current.key == MoviesTab.key,//selectedNavItem == index
                        onClick = { tabNavigator.current = MoviesTab },
                    )

                    NavigationBarItem(
                        icon = {
                            Icon(
                                painterResource("baseline_tv_24.xml"),
                                contentDescription = MoviesTab.Tabs.TV_SHOWS,
                            )
                        },
                        label = { Text(MoviesTab.Tabs.TV_SHOWS) },
                        selected = tabNavigator.current.key == TvShowsTab.key,//selectedNavItem == index
                        onClick = { tabNavigator.current = TvShowsTab },
                    )
                }
            }
        }) { innerPadding ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(innerPadding).background(md_theme_dark_surface)
            ) {
                if (windowSize == WindowSize.EXPANDED) {

                    NavigationRail(content = {
                        NavigationRailItem(
                            modifier = Modifier.padding(8.dp),
                            icon = {
                                Icon(
                                    painterResource("baseline_movie_24.xml"),
                                    contentDescription = MoviesTab.Tabs.MOVIES,
                                )
                            },
                            label = { Text(MoviesTab.Tabs.MOVIES) },
                            selected = tabNavigator.current.key == MoviesTab.key,
                            onClick = { tabNavigator.current = MoviesTab },
                        )
                        NavigationRailItem(
                            modifier = Modifier.padding(8.dp),
                            icon = {
                                Icon(
                                    painterResource("baseline_tv_24.xml"),
                                    contentDescription = MoviesTab.Tabs.TV_SHOWS,
                                )
                            },
                            label = { Text(MoviesTab.Tabs.TV_SHOWS) },
                            selected = tabNavigator.current.key == TvShowsTab.key,
                            onClick = { tabNavigator.current = TvShowsTab },
                        )
                    })
                }
                Column {
                    if (windowSize == WindowSize.EXPANDED || windowSize == WindowSize.MEDIUM) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SearchBox(
                                hint = "Search Movies, Tv shows, Person",
                                modifier = Modifier.wrapContentHeight()
                            ) { }
                        }

                    }
                    CurrentTab()
                }
            }
        }
    }
}

private fun getWindowSize(windowSizeClass: WindowSizeClass) =
    WindowSize.basedOnWindowSizeClass(windowSizeClass.widthSizeClass.toString())

internal expect fun openUrl(url: String?)


