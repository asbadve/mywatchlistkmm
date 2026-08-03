@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package com.ajinkyabadve.kmmmywatchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import coil3.compose.setSingletonImageLoaderFactory
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.image.newImageLoader
import com.ajinkyabadve.kmmmywatchlist.design.searchbox.SearchBox
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MovieScreenTabs
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.CollectionDetailScreen
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.MovieDetailScreen
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail.PersonDetailScreen
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.MyFavScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.PersonScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.TrendingScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.TvShowsScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail.AllSeasonsScreen
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail.EpisodeDetailScreen
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail.EpisodeListScreen
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail.TvDetailScreen
import com.ajinkyabadve.kmmmywatchlist.navigation.AllSeasonsKey
import com.ajinkyabadve.kmmmywatchlist.navigation.AppKey
import com.ajinkyabadve.kmmmywatchlist.navigation.CollectionDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.EpisodeDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.EpisodeListKey
import com.ajinkyabadve.kmmmywatchlist.navigation.MovieDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.MoviesKey
import com.ajinkyabadve.kmmmywatchlist.navigation.MyFavKey
import com.ajinkyabadve.kmmmywatchlist.navigation.NavigationConstants
import com.ajinkyabadve.kmmmywatchlist.navigation.PersonDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.PersonKey
import com.ajinkyabadve.kmmmywatchlist.navigation.TopLevelBackStack
import com.ajinkyabadve.kmmmywatchlist.navigation.TrendingKey
import com.ajinkyabadve.kmmmywatchlist.navigation.TvDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.TvShowsKey
import com.ajinkyabadve.kmmmywatchlist.navigation.bottomNavItems
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.coming_soon_search_message
import mywatchlist.composeapp.generated.resources.coming_soon_title
import mywatchlist.composeapp.generated.resources.placeholder_select_season
import mywatchlist.composeapp.generated.resources.search_hint
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun App(calculateWindowSizeClass: WindowSizeClass) {
    setSingletonImageLoaderFactory { context ->
        newImageLoader(context)
    }
    // Refresh configuration dynamically on app launch
    LaunchedEffect(Unit) {
        ImageConfigResolver.refreshConfig()
    }
    // NavDisplay disposes an entry's composition once it's no longer the top of the backstack
    // (e.g. TvDetail -> AllSeasons), so viewModel() calls inside it rely entirely on the ambient
    // ViewModelStoreOwner to keep their ViewModel alive across that disposal. Provide one stable
    // owner for the whole app session here instead of relying on per-platform defaults, so
    // navigating away and back doesn't recreate ViewModels and refetch already-loaded data.
    val appViewModelStoreOwner =
        remember {
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = ViewModelStore()
            }
        }
    CompositionLocalProvider(LocalViewModelStoreOwner provides appViewModelStoreOwner) {
        AppTheme {
            val windowSize = WindowSize.getWindowSize(calculateWindowSizeClass)
            MainAppScreen(windowSize)
        }
    }
}

@Composable
fun MainAppScreen(windowSize: WindowSize) {
    val topLevelBackStack = remember { TopLevelBackStack(TrendingKey) }
    val currentKey = topLevelBackStack.backStack.lastOrNull()

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType =
        if (windowSize.isExpanded() && !adaptiveInfo.windowPosture.isTabletop) {
            NavigationSuiteType.NavigationRail
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
                    bottomNavItems.forEach { item ->
                        val selected = topLevelBackStack.topLevelKey == item.key
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = {
                                topLevelBackStack.switchTopLevel(item.key)
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
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
                    bottomNavItems.forEach { item ->
                        val selected = topLevelBackStack.topLevelKey == item.key
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                topLevelBackStack.switchTopLevel(item.key)
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            modifier = Modifier.padding(vertical = NavigationConstants.NAVIGATION_RAIL_ITEM_VERTICAL_PADDING),
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            MainAppScaffoldContent(
                windowSize = windowSize,
                topLevelBackStack = topLevelBackStack,
                layoutType = layoutType,
                currentKey = currentKey,
            )
        }
    }
}

@Composable
private fun MainAppScaffoldContent(
    windowSize: WindowSize,
    topLevelBackStack: TopLevelBackStack,
    layoutType: NavigationSuiteType,
    currentKey: AppKey?,
) {
    val showTopBar =
        when (currentKey) {
            TrendingKey, MoviesKey, TvShowsKey, PersonKey, MyFavKey -> true
            else -> false
        }

    val topBarContent: @Composable () -> Unit =
        if (showTopBar) {
            { AppTopBar(windowSize) }
        } else {
            {}
        }

    Scaffold(
        topBar = topBarContent,
        bottomBar = {
            if (layoutType != NavigationSuiteType.NavigationDrawer && layoutType != NavigationSuiteType.NavigationRail) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = topLevelBackStack.topLevelKey == item.key
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                topLevelBackStack.switchTopLevel(item.key)
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(item.icon),
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        val listDetailStrategy = rememberListDetailSceneStrategy<AppKey>()
        NavDisplay(
            backStack = topLevelBackStack.backStack,
            onBack = { topLevelBackStack.removeLast() },
            sceneStrategies = listOf(listDetailStrategy),
            modifier =
                Modifier.padding(
                    top =
                        if (currentKey !is MovieDetailKey &&
                            currentKey !is CollectionDetailKey &&
                            currentKey !is TvDetailKey &&
                            currentKey !is PersonDetailKey &&
                            currentKey !is AllSeasonsKey &&
                            currentKey !is EpisodeListKey &&
                            currentKey !is EpisodeDetailKey
                        ) {
                            innerPadding.calculateTopPadding()
                        } else {
                            0.dp
                        },
                    bottom = innerPadding.calculateBottomPadding(),
                ),
            entryProvider =
                entryProvider {
                    entry<TrendingKey> {
                        TrendingScreenTab(
                            onMovieSelected = { movieId ->
                                topLevelBackStack.add(MovieDetailKey(movieId))
                            },
                            onTvShowSelected = { tvShowId ->
                                topLevelBackStack.add(TvDetailKey(tvShowId))
                            },
                            onPersonSelected = { personId ->
                                topLevelBackStack.add(PersonDetailKey(personId))
                            },
                        )
                    }
                    entry<MoviesKey> {
                        MovieScreenTabs(
                            modifier = Modifier,
                            onMovieSelected = { movieId ->
                                topLevelBackStack.add(MovieDetailKey(movieId))
                            },
                        )
                    }
                    entry<TvShowsKey> {
                        TvShowsScreenTab(
                            onTvShowSelected = { tvShowId ->
                                topLevelBackStack.add(TvDetailKey(tvShowId))
                            },
                        )
                    }
                    entry<PersonKey> {
                        PersonScreenTab(
                            onPersonSelected = { personId ->
                                topLevelBackStack.add(PersonDetailKey(personId))
                            },
                        )
                    }
                    entry<MyFavKey> { MyFavScreenTab() }
                    entry<MovieDetailKey> { key ->
                        MovieDetailScreen(
                            movieId = key.movieId,
                            windowSize = windowSize,
                            onBackClicked = { topLevelBackStack.removeLast() },
                            onMovieClicked = { nextMovieId ->
                                topLevelBackStack.add(MovieDetailKey(nextMovieId))
                            },
                            onPersonClicked = { personId ->
                                topLevelBackStack.add(PersonDetailKey(personId))
                            },
                            onCollectionClicked = { collectionId ->
                                topLevelBackStack.add(CollectionDetailKey(collectionId))
                            },
                        )
                    }
                    entry<CollectionDetailKey> { key ->
                        CollectionDetailScreen(
                            collectionId = key.collectionId,
                            windowSize = windowSize,
                            onBackClicked = { topLevelBackStack.removeLast() },
                            onMovieClicked = { movieId ->
                                topLevelBackStack.add(MovieDetailKey(movieId))
                            },
                            onPersonClicked = { personId ->
                                topLevelBackStack.add(PersonDetailKey(personId))
                            },
                        )
                    }
                    entry<TvDetailKey> { key ->
                        TvDetailScreen(
                            tvShowId = key.tvShowId,
                            windowSize = windowSize,
                            onBackClicked = { topLevelBackStack.removeLast() },
                            onTvShowClicked = { nextTvShowId ->
                                topLevelBackStack.add(TvDetailKey(nextTvShowId))
                            },
                            onViewAllSeasonsClick = { seasonsTvShowId ->
                                topLevelBackStack.add(AllSeasonsKey(seasonsTvShowId))
                            },
                            onPersonClicked = { personId ->
                                topLevelBackStack.add(PersonDetailKey(personId))
                            },
                        )
                    }
                    entry<PersonDetailKey> { key ->
                        PersonDetailScreen(
                            personId = key.personId,
                            windowSize = windowSize,
                            onBackClicked = { topLevelBackStack.removeLast() },
                            onMovieClicked = { movieId ->
                                topLevelBackStack.add(MovieDetailKey(movieId))
                            },
                            onTvShowClicked = { tvShowId ->
                                topLevelBackStack.add(TvDetailKey(tvShowId))
                            },
                        )
                    }
                    entry<AllSeasonsKey>(
                        metadata =
                            ListDetailSceneStrategy.listPane(
                                detailPlaceholder = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(stringResource(Res.string.placeholder_select_season))
                                    }
                                },
                            ),
                    ) { key ->
                        AllSeasonsScreen(
                            tvShowId = key.tvShowId,
                            onBackClicked = { topLevelBackStack.removeLast() },
                            onSeasonClicked = { seasonNumber ->
                                topLevelBackStack.add(EpisodeListKey(key.tvShowId, seasonNumber))
                            },
                        )
                    }
                    entry<EpisodeListKey>(
                        metadata = ListDetailSceneStrategy.detailPane(),
                    ) { key ->
                        EpisodeListScreen(
                            tvShowId = key.tvShowId,
                            seasonNumber = key.seasonNumber,
                            onBackClicked = { topLevelBackStack.removeLast() },
                            onEpisodeClicked = { episodeNumber ->
                                topLevelBackStack.add(EpisodeDetailKey(key.tvShowId, key.seasonNumber, episodeNumber))
                            },
                        )
                    }
                    entry<EpisodeDetailKey> { key ->
                        EpisodeDetailScreen(
                            tvShowId = key.tvShowId,
                            seasonNumber = key.seasonNumber,
                            episodeNumber = key.episodeNumber,
                            windowSize = windowSize,
                            onBackClicked = { topLevelBackStack.removeLast() },
                            onPersonClicked = { personId ->
                                topLevelBackStack.add(PersonDetailKey(personId))
                            },
                        )
                    }
                },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(windowSize: WindowSize) {
    // Search isn't implemented yet - clicking the search box surfaces a "Coming Soon" notice
    // instead of a dead-end no-op.
    var showSearchComingSoon by remember { mutableStateOf(false) }

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
                    hint = stringResource(Res.string.search_hint),
                    onClick = { showSearchComingSoon = true },
                )
            }
        },
    )

    if (showSearchComingSoon) {
        AlertDialog(
            onDismissRequest = { showSearchComingSoon = false },
            title = { Text(stringResource(Res.string.coming_soon_title)) },
            text = { Text(stringResource(Res.string.coming_soon_search_message)) },
            confirmButton = {
                TextButton(onClick = { showSearchComingSoon = false }) {
                    Text(stringResource(Res.string.action_close))
                }
            },
        )
    }
}

internal expect fun openUrl(url: String?)
