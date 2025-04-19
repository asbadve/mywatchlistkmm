@file:OptIn(
    ExperimentalMaterial3Api::class,
)

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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabDisposable
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.design.searchbox.SearchBox
import com.ajinkyabadve.kmmmywatchlist.homepage.model.AppTabs
import com.ajinkyabadve.kmmmywatchlist.homepage.model.HomeNavigation
import com.ajinkyabadve.kmmmywatchlist.homepage.model.TabNavigation
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.FavTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.PersonTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TrendingTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TvShowsTab
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_surface
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_light_surface
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun App(calculateWindowSizeClass: WindowSizeClass) =
    AppTheme {
        val windowSize = WindowSize.getWindowSize(calculateWindowSizeClass)
        val navigationList = TabNavigation.getNavigation()
        TabNavigator(
            tab = TrendingTab,
            tabDisposable = {
                TabDisposable(
                    navigator = it,
                    tabs = AppTabs.getTabs(),
                )
            },
        ) { tabNavigator ->
            Scaffold(
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                topBar = {
                    topAppBar(windowSize)
                },
                bottomBar = {
                    bottomNavigation(
                        windowSize = windowSize,
                        tabNavigator = tabNavigator,
                        navigationList = navigationList,
                    )
                },
            ) { innerPadding ->
                appScreenContent(
                    innerPadding = innerPadding,
                    windowSize = windowSize,
                    tabNavigator = tabNavigator,
                    navigationList = navigationList,
                )
            }
        }
    }

@Composable
private fun appScreenContent(
    innerPadding: PaddingValues,
    windowSize: WindowSize,
    tabNavigator: TabNavigator,
    navigationList: List<HomeNavigation>,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier =
            Modifier.padding(innerPadding)
                .background(if (isSystemInDarkTheme()) md_theme_dark_surface else md_theme_light_surface),
    ) {
        if (windowSize == WindowSize.EXPANDED) {
            NavigationRail(
                content =
                    navigationRailContent(
                        tabNavigator = tabNavigator,
                        navigationList = navigationList,
                    ),
            )
        }
        CurrentTab()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
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

private fun navigationRailContent(
    tabNavigator: TabNavigator,
    navigationList: List<HomeNavigation>,
): @Composable
(ColumnScope.() -> Unit) =
    {
        Spacer(Modifier.weight(1f))
        navigationList.forEachIndexed { index, homeNavigation ->
            NavigationRailItem(
                modifier = Modifier.padding(8.dp),
                icon = {
                    Icon(
                        painterResource(homeNavigation.drawableResource),
                        contentDescription = homeNavigation.iconContentDescription,
                    )
                },
                label = { Text(homeNavigation.label) },
                selected = isTabSelected(tabNavigator = tabNavigator, label = homeNavigation.label),
                onClick = onNavigationClick(homeNavigation, tabNavigator),
            )
            if (index != 0 || index != navigationList.size - 1) {
                Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.weight(1f))
    }

@Composable
private fun bottomNavigation(
    windowSize: WindowSize,
    tabNavigator: TabNavigator,
    navigationList: List<HomeNavigation>,
) {
    if (windowSize.isCompact() || windowSize.isMedium()) {
        NavigationBar(windowInsets = WindowInsets.navigationBars) {
            navigationList.forEachIndexed { index, homeNavigation ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            painterResource(homeNavigation.drawableResource),
                            contentDescription = homeNavigation.iconContentDescription,
                        )
                    },
                    label = { Text(homeNavigation.label) },
                    selected = isTabSelected(tabNavigator, homeNavigation.label),
                    onClick = onNavigationClick(homeNavigation, tabNavigator),
                )
            }
        }
    }
}

private fun onNavigationClick(
    homeNavigation: HomeNavigation,
    tabNavigator: TabNavigator,
): () -> Unit =
    when (homeNavigation.label) {
        AppTabs.MOVIES -> {
            {
                tabNavigator.current = MoviesTab
            }
        }

        AppTabs.TV_SHOWS -> {
            {
                tabNavigator.current = TvShowsTab
            }
        }

        AppTabs.PERSON -> {
            {
                tabNavigator.current = PersonTab
            }
        }

        AppTabs.TRENDING -> {
            {
                tabNavigator.current = TrendingTab
            }
        }

        AppTabs.FAV -> {
            {
                tabNavigator.current = FavTab
            }
        }

        else -> {
            throw IllegalArgumentException("Handle navigation click item" + homeNavigation.label)
        }
    }

fun isTabSelected(
    tabNavigator: TabNavigator,
    label: String,
): Boolean =
    when (label) {
        AppTabs.MOVIES -> tabNavigator.current.key == MoviesTab.key
        AppTabs.TV_SHOWS -> tabNavigator.current.key == TvShowsTab.key
        AppTabs.PERSON -> tabNavigator.current.key == PersonTab.key
        AppTabs.TRENDING -> tabNavigator.current.key == TrendingTab.key
        AppTabs.FAV -> tabNavigator.current.key == FavTab.key
        else -> {
            false
        }
    }
