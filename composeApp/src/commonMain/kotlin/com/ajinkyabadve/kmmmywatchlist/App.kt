@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.ajinkyabadve.kmmmywatchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.AppTabs
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TvShowsTab
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_surface
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_light_surface
import org.jetbrains.compose.resources.painterResource

@Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming")
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@ExperimentalMaterial3Api
@Composable
internal fun App() =
    AppTheme {
        val windowSize = WindowSize.getWindowSize(calculateWindowSizeClass())
        TabNavigator(MoviesTab, tabDisposable = {
            TabDisposable(
                navigator = it,
                tabs = listOf(MoviesTab, TvShowsTab),
            )
        }) { tabNavigator ->
            Scaffold(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                topBar = {
                    topAppBar(windowSize)
                },
                bottomBar = {
                    bottomAppBar(windowSize, tabNavigator)
                },
            ) { innerPadding ->
                appScreenContent(innerPadding, windowSize, tabNavigator)
            }
        }
    }

@Composable
private fun appScreenContent(
    innerPadding: PaddingValues,
    windowSize: WindowSize,
    tabNavigator: TabNavigator,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier =
            Modifier.padding(innerPadding)
                .background(if (isSystemInDarkTheme()) md_theme_dark_surface else md_theme_light_surface),
    ) {
        if (windowSize == WindowSize.EXPANDED) {
            NavigationRail(
                content = navigationRailContent(tabNavigator),
            )
        }
        CurrentTab()
    }
}

private fun navigationRailContent(tabNavigator: TabNavigator): @Composable
(ColumnScope.() -> Unit) =
    {
        Spacer(Modifier.weight(1f))
        NavigationRailItem(
            modifier = Modifier.padding(8.dp),
            icon = {
                Icon(
                    painterResource("baseline_movie_24.xml"),
                    contentDescription = AppTabs.MOVIES,
                )
            },
            label = { Text(AppTabs.MOVIES) },
            selected = tabNavigator.current.key == MoviesTab.key,
            onClick = { tabNavigator.current = MoviesTab },
        )
        Spacer(Modifier.weight(1f))
        NavigationRailItem(
            modifier = Modifier.padding(8.dp),
            icon = {
                Icon(
                    painterResource("baseline_tv_24.xml"),
                    contentDescription = AppTabs.TV_SHOWS,
                )
            },
            label = { Text(AppTabs.TV_SHOWS) },
            selected = tabNavigator.current.key == TvShowsTab.key,
            onClick = { tabNavigator.current = TvShowsTab },
        )
        Spacer(Modifier.weight(1f))
    }

@Composable
private fun bottomAppBar(
    windowSize: WindowSize,
    tabNavigator: TabNavigator,
) {
    if (windowSize.isCompact() || windowSize.isMedium()) {
        NavigationBar {
            NavigationBarItem(
                icon = {
                    Icon(
                        painterResource("baseline_movie_24.xml"),
                        contentDescription = AppTabs.MOVIES,
                    )
                },
                label = { Text(AppTabs.MOVIES) },
                selected = tabNavigator.current.key == MoviesTab.key, // selectedNavItem == index
                onClick = { tabNavigator.current = MoviesTab },
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        painterResource("baseline_tv_24.xml"),
                        contentDescription = AppTabs.TV_SHOWS,
                    )
                },
                label = { Text(AppTabs.TV_SHOWS) },
                selected = tabNavigator.current.key == TvShowsTab.key, // selectedNavItem == index
                onClick = { tabNavigator.current = TvShowsTab },
            )
        }
    }
}

@Composable
@ExperimentalMaterial3Api
private fun topAppBar(windowSize: WindowSize) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SearchBox(
                        modifier =
                            if (windowSize.isExpanded() || windowSize.isMedium()) {
                                Modifier.wrapContentHeight()
                                    .align(Alignment.CenterHorizontally)
                            } else {
                                Modifier.fillMaxWidth().padding(end = 8.dp).wrapContentHeight()
                                    .align(Alignment.CenterHorizontally)
                            },
                        hint = "Search for Movies & Tv shows",
                        onClick = {},
                    )
                }
            },
        )
    }
}

internal expect fun openUrl(url: String?)
