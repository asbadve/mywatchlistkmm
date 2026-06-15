@file:OptIn(
    ExperimentalMaterial3Api::class,
)

package com.ajinkyabadve.kmmmywatchlist

import HomepageScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import bottomNavItems
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.design.searchbox.SearchBox
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MovieScreenTabs
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.MovieListScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.MyFavScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.PersonScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.TrendingScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.TrendingScreenTabViewModel
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.TvShowsScreenTab
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.navigation.NavigationConstants
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor2.KtorNetworkFetcherFactory
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun App(calculateWindowSizeClass: WindowSizeClass) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }
    AppTheme {
        val windowSize = WindowSize.getWindowSize(calculateWindowSizeClass)
        MainAppScreen(windowSize)
    }
}

@Composable
fun MainAppScreen(
    windowSize: WindowSize,
    nowPlayingViewModel: MovieListScreenModel = remember { MovieListScreenModel(MoviesConstant.NOW_PLAYING_API_PATH) },
    upcomingViewModel: MovieListScreenModel = remember { MovieListScreenModel(MoviesConstant.UPCOMING_API_PATH) },
    popularViewModel: MovieListScreenModel = remember { MovieListScreenModel(MoviesConstant.POPULAR_API_PATH) },
    topRatedViewModel: MovieListScreenModel = remember { MovieListScreenModel(MoviesConstant.TOP_RATED_API_PATH) },
    trendingViewModel: TrendingScreenTabViewModel = viewModel { TrendingScreenTabViewModel() },
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType =
        if (windowSize.isExpanded() && !adaptiveInfo.windowPosture.isTabletop) {
            NavigationSuiteType.NavigationDrawer
        } else {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
        }

    Row(modifier = Modifier.fillMaxSize()) {
        if (layoutType == NavigationSuiteType.NavigationDrawer) {
            PermanentDrawerSheet(
                modifier = Modifier.width(NavigationConstants.NAVIGATION_DRAWER_WIDTH),
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                drawerShape = androidx.compose.ui.graphics.RectangleShape,
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = {
                                navigateWithSingleTop(navController, screen.route)
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(screen.icon),
                                    contentDescription = screen.label,
                                )
                            },
                            label = { Text(screen.label) },
                            modifier =
                                Modifier
                                    .padding(horizontal = NavigationConstants.NAVIGATION_DRAWER_ITEM_HORIZONTAL_PADDING)
                                    .padding(vertical = NavigationConstants.NAVIGATION_DRAWER_ITEM_VERTICAL_PADDING),
                        )
                    }
                }
            }
        } else if (layoutType == NavigationSuiteType.NavigationRail) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navigateWithSingleTop(navController, screen.route)
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(screen.icon),
                                    contentDescription = screen.label,
                                )
                            },
                            label = { Text(screen.label) },
                            modifier = Modifier.padding(vertical = NavigationConstants.NAVIGATION_RAIL_ITEM_VERTICAL_PADDING),
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            MainAppScaffoldContent(
                windowSize = windowSize,
                navController = navController,
                layoutType = layoutType,
                currentDestination = currentDestination,
                nowPlayingViewModel = nowPlayingViewModel,
                upcomingViewModel = upcomingViewModel,
                popularViewModel = popularViewModel,
                topRatedViewModel = topRatedViewModel,
                trendingViewModel = trendingViewModel,
            )
        }
    }
}

@Composable
private fun MainAppScaffoldContent(
    windowSize: WindowSize,
    navController: NavHostController,
    layoutType: NavigationSuiteType,
    currentDestination: NavDestination?,
    nowPlayingViewModel: MovieListScreenModel,
    upcomingViewModel: MovieListScreenModel,
    popularViewModel: MovieListScreenModel,
    topRatedViewModel: MovieListScreenModel,
    trendingViewModel: TrendingScreenTabViewModel,
) {
    Scaffold(
        topBar = {
            AppTopBar(windowSize)
        },
        bottomBar = {
            if (layoutType != NavigationSuiteType.NavigationDrawer && layoutType != NavigationSuiteType.NavigationRail) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navigateWithSingleTop(navController, screen.route)
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(screen.icon),
                                    contentDescription = screen.label,
                                )
                            },
                            label = { Text(screen.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomepageScreen.Trending.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(HomepageScreen.Trending.route) { TrendingScreenTab(viewModel = trendingViewModel) }
            composable(HomepageScreen.Movies.route) {
                MovieScreenTabs(
                    modifier = Modifier,
                    nowPlayingViewModel = nowPlayingViewModel,
                    upcomingViewModel = upcomingViewModel,
                    popularViewModel = popularViewModel,
                    topRatedViewModel = topRatedViewModel,
                    onMovieSelected = {},
                )
            }
            composable(HomepageScreen.Tvshows.route) { TvShowsScreenTab() }
            composable(HomepageScreen.Person.route) { PersonScreenTab() }
            composable(HomepageScreen.MyFav.route) { MyFavScreenTab() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(windowSize: WindowSize) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
        title = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(end = NavigationConstants.TOP_BAR_END_PADDING),
                contentAlignment = Alignment.Center,
            ) {
                SearchBox(
                    modifier =
                        Modifier.fillMaxWidth(
                            if (windowSize.isCompact()) {
                                NavigationConstants.SEARCH_BOX_COMPACT_WIDTH_FRACTION
                            } else {
                                NavigationConstants.SEARCH_BOX_WIDE_WIDTH_FRACTION
                            },
                        ),
                    hint = "Search for Movies & Tv shows",
                    onClick = {},
                )
            }
        },
    )
}

private fun navigateWithSingleTop(
    navController: NavHostController,
    route: String,
) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

internal expect fun openUrl(url: String?)
