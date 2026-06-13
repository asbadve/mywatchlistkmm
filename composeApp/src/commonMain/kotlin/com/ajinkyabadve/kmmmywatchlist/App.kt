@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.ajinkyabadve.kmmmywatchlist

import HomepageScreen
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import bottomNavItems
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabDisposable
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.design.searchbox.SearchBox
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MovieScreenTabs
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.MyFavScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.PersonScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.TrendingScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.TvShowsScreenTab
import com.ajinkyabadve.kmmmywatchlist.homepage.model.AppTabs
import com.ajinkyabadve.kmmmywatchlist.homepage.model.HomeNavigation
import com.ajinkyabadve.kmmmywatchlist.homepage.model.TabNavigation
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.FavTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.PersonTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TrendingTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TvShowsTab
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun App(calculateWindowSizeClass: WindowSizeClass) =
    AppTheme {
        val windowSize = WindowSize.getWindowSize(calculateWindowSizeClass)
        val navigationList = TabNavigation.getNavigation()
        //todo work on this
        MainAppScreen(windowSize)
//        Old homepage
//        mainScreen(windowSize, navigationList)
    }


@Composable
fun MainAppScreen(windowSize: WindowSize) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType = if (windowSize.isExpanded() && !adaptiveInfo.windowPosture.isTabletop) {
        NavigationSuiteType.NavigationDrawer
    } else {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
            adaptiveInfo
        )
    }
    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            bottomNavItems.forEachIndexed { index, screen ->
                item(
                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                    onClick = {
                        navController.navigate(screen.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            painterResource(screen.icon),
                            contentDescription = screen.label,
                        )
                    },
                    label = { Text(screen.label) },
                    modifier = Modifier.padding(
                        top = if (index == 0) 18.dp else 0.dp,
                        bottom = if (index == bottomNavItems.size - 1) 18.dp else 0.dp
                    )
                )
            }
        },
    ) {
        Scaffold(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            topBar = {
                topAppBar(windowSize)
            },
        ) { innerPadding -> // Content of the Scaffold
            NavHost(
                navController = navController,
                startDestination = HomepageScreen.Trending.route,
                modifier = Modifier.padding(innerPadding) // Apply padding from Scaffold
            ) {
                composable(HomepageScreen.Trending.route) { TrendingScreenTab() }
                composable(HomepageScreen.Movies.route) {
//                    MovieScreen()
                    MovieScreenTabs(Modifier, onMovieSelected = {
//                        navController.navigate(HomepageScreen.MovieDetail.route)
                    })
                }
                composable(HomepageScreen.Tvshows.route) { TvShowsScreenTab() }
                composable(HomepageScreen.Person.route) { PersonScreenTab() }
                composable(HomepageScreen.MyFav.route) { MyFavScreenTab() }
            }
        }

    }

}

@Composable
private fun mainScreen(
    windowSize: WindowSize,
    navigationList: List<HomeNavigation>
) {
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

//                .background(if (isSystemInDarkTheme()) md_theme_dark_surface else md_theme_light_surface),
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
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//    ) {
    TopAppBar(
        colors = TopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            navigationIconContentColor = TopAppBarDefaults.topAppBarColors().navigationIconContentColor,
            titleContentColor = TopAppBarDefaults.topAppBarColors().titleContentColor,
            actionIconContentColor = TopAppBarDefaults.topAppBarColors().actionIconContentColor,
        ),
        title = {
            SearchBox(
                modifier = Modifier,
//                        modifier =
//                            if (windowSize.isExpanded() || windowSize.isMedium()) {
//                                Modifier.wrapContentHeight()
//                                    .align(Alignment.CenterHorizontally)
//                            } else {
//                                Modifier.fillMaxWidth().padding(end = 8.dp).wrapContentHeight()
//                                    .align(Alignment.CenterHorizontally)
//                            },
                hint = "Search for Movies & Tv shows",
                onClick = {},
            )
//                }
        },
    )
//    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    searchResults: List<String>,
    modifier: Modifier = Modifier
) {
    // Controls expansion state of the search bar
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = { textFieldState.edit { replace(0, length, it) } },
                    onSearch = {
                        onSearch(textFieldState.text.toString())
                        expanded = false
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("Search") }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            // Display search results in a scrollable column
            Column(Modifier.verticalScroll(rememberScrollState())) {
                searchResults.forEach { result ->
                    ListItem(
                        headlineContent = { Text(result) },
                        modifier = Modifier
                            .clickable {
                                textFieldState.edit { replace(0, length, result) }
                                expanded = false
                            }
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}
