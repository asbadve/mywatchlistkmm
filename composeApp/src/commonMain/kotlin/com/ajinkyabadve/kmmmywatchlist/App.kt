@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class,
)

package com.ajinkyabadve.kmmmywatchlist

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import coil3.compose.setSingletonImageLoaderFactory
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.auth.rememberWebAuthLauncher
import com.ajinkyabadve.kmmmywatchlist.core.image.newImageLoader
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.AccountAvatarButton
import com.ajinkyabadve.kmmmywatchlist.core.ui.auth.SessionExpiredDialog
import com.ajinkyabadve.kmmmywatchlist.core.ui.collapsingFooter
import com.ajinkyabadve.kmmmywatchlist.core.ui.rememberCollapsibleBarState
import com.ajinkyabadve.kmmmywatchlist.design.searchbox.SearchBox
import com.ajinkyabadve.kmmmywatchlist.features.account.screen.ListDetailScreen
import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.auth.screen.AccountScreen
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MovieScreenTabs
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.CollectionDetailScreen
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.MovieDetailScreen
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail.PersonDetailScreen
import com.ajinkyabadve.kmmmywatchlist.features.search.screen.SearchScreen
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.MyFavScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.PersonScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.TrendingScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.trending.screen.TvShowsScreenTab
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail.AllSeasonsScreen
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail.EpisodeDetailScreen
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail.EpisodeListScreen
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail.TvDetailScreen
import com.ajinkyabadve.kmmmywatchlist.navigation.AccountKey
import com.ajinkyabadve.kmmmywatchlist.navigation.AllSeasonsKey
import com.ajinkyabadve.kmmmywatchlist.navigation.AppKey
import com.ajinkyabadve.kmmmywatchlist.navigation.CollectionDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.EpisodeDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.EpisodeListKey
import com.ajinkyabadve.kmmmywatchlist.navigation.ListDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.MovieDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.MoviesKey
import com.ajinkyabadve.kmmmywatchlist.navigation.MyFavKey
import com.ajinkyabadve.kmmmywatchlist.navigation.NavigationConstants
import com.ajinkyabadve.kmmmywatchlist.navigation.PersonDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.PersonKey
import com.ajinkyabadve.kmmmywatchlist.navigation.SearchKey
import com.ajinkyabadve.kmmmywatchlist.navigation.TopLevelBackStack
import com.ajinkyabadve.kmmmywatchlist.navigation.TrendingKey
import com.ajinkyabadve.kmmmywatchlist.navigation.TvDetailKey
import com.ajinkyabadve.kmmmywatchlist.navigation.TvShowsKey
import com.ajinkyabadve.kmmmywatchlist.navigation.bottomNavItems
import com.ajinkyabadve.kmmmywatchlist.navigation.isDetailKey
import com.ajinkyabadve.kmmmywatchlist.network.exception.HttpExceptions
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import io.github.aakira.napier.Napier
import io.ktor.serialization.ContentConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import mywatchlist.composeapp.generated.resources.Res
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
    val authRepository: AuthRepository = remember { AuthRepositoryImpl() }
    val session by authRepository.sessionState.collectAsState()
    val showSessionExpiredDialog = remember { androidx.compose.runtime.mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val webAuthLauncher = rememberWebAuthLauncher()
    LaunchedEffect(webAuthLauncher) {
        webAuthLauncher.checkPendingAuth { token, approved ->
            if (approved && token.isNotEmpty()) {
                coroutineScope.launch {
                    try {
                        authRepository.createSession(token)
                    } catch (e: HttpExceptions) {
                        Napier.e(tag = "MainAppScaffoldContent", throwable = e) { "Http error creating session from web callback" }
                    } catch (e: IOException) {
                        Napier.e(tag = "MainAppScaffoldContent", throwable = e) { "Network error creating session from web callback" }
                    } catch (e: ContentConvertException) {
                        Napier.e(
                            tag = "MainAppScaffoldContent",
                            throwable = e,
                        ) { "Content conversion error creating session from web callback" }
                    } catch (e: SerializationException) {
                        Napier.e(tag = "MainAppScaffoldContent", throwable = e) { "Serialization error creating session from web callback" }
                    }
                }
            }
        }
    }

    LaunchedEffect(authRepository) {
        authRepository.sessionExpiredEvent.collect {
            showSessionExpiredDialog.value = true
        }
    }

    // Account behaves differently by presentation: as a compact full-screen page it brings its own
    // header (same as Search/detail screens) and the shared top bar hides. As an expanded dialog it
    // floats over the tab content, so the shared top bar underneath stays visible and reachable.
    val showTopBar =
        when (currentKey) {
            TrendingKey, MoviesKey, TvShowsKey, PersonKey, MyFavKey -> true
            AccountKey -> !windowSize.isCompact()
            else -> false
        }

    val isDetailScreen = currentKey.isDetailKey()

    // Every scrolling screen hands its chrome back to the reader: the search bar and the nav bar
    // both leave on the way down and return on the way up. Detail screens have no app-level top bar
    // of their own to collapse - they bring their own, which collapses the same way.
    //
    // The search bar lives in Scaffold's topBar slot, so Material3 can drive it and we get its
    // fling/settling animation for free. NavigationBar has no scrollBehavior parameter of its own
    // (and NavigationSuiteScaffoldState does not exist in Compose Multiplatform yet), so the nav
    // bar is the one that still needs a hand-rolled connection.
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val bottomBarState = rememberCollapsibleBarState()
    LaunchedEffect(currentKey) {
        topBarScrollBehavior.state.heightOffset = 0f
        bottomBarState.reset()
    }

    val topBarContent: @Composable () -> Unit =
        if (showTopBar) {
            {
                AppTopBar(
                    windowSize = windowSize,
                    session = session,
                    onSearchClicked = { topLevelBackStack.add(SearchKey) },
                    onAccountClicked = {
                        // Tapping the avatar again while Account is already open closes it,
                        // instead of pushing a second copy onto the back stack.
                        if (currentKey == AccountKey) {
                            topLevelBackStack.removeLast()
                        } else {
                            topLevelBackStack.add(AccountKey)
                        }
                    },
                    scrollBehavior = topBarScrollBehavior,
                )
            }
        } else {
            {}
        }

    Scaffold(
        // Order matters: the nav bar's connection is outermost so it sees the whole delta, while
        // enterAlwaysScrollBehavior consumes what it uses to move the search bar.
        modifier =
            Modifier
                .nestedScroll(bottomBarState.nestedScrollConnection)
                .nestedScroll(topBarScrollBehavior.nestedScrollConnection),
        topBar = topBarContent,
        bottomBar = {
            if (layoutType != NavigationSuiteType.NavigationDrawer && layoutType != NavigationSuiteType.NavigationRail) {
                NavigationBar(
                    // Collapse only the bar's own chrome: NavigationBar is what consumes the
                    // bottom system inset, so letting it reach zero would slide the content
                    // under the gesture bar.
                    modifier =
                        Modifier.collapsingFooter(
                            state = bottomBarState,
                            minVisibleHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                        ),
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
        // Overlay strategies (Dialog) must come before non-overlay ones so they get first refusal
        // on rendering an entry - see DialogSceneStrategy's kdoc.
        val dialogStrategy = remember { DialogSceneStrategy<AppKey>() }
        NavDisplay(
            backStack = topLevelBackStack.backStack,
            onBack = { topLevelBackStack.removeLast() },
            sceneStrategies = listOf(dialogStrategy, listDetailStrategy),
            // Both bars shrink their reported height as they collapse, so innerPadding already
            // tracks them and the content grows into the space instead of leaving a dead strip.
            modifier =
                Modifier.padding(
                    top = if (isDetailScreen) 0.dp else innerPadding.calculateTopPadding(),
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
                    entry<MyFavKey> {
                        MyFavScreenTab(
                            authRepository = authRepository,
                            onMovieSelected = { movieId ->
                                topLevelBackStack.add(MovieDetailKey(movieId))
                            },
                            onTvSelected = { tvId ->
                                topLevelBackStack.add(TvDetailKey(tvId))
                            },
                            onListSelected = { listId ->
                                topLevelBackStack.add(ListDetailKey(listId))
                            },
                        )
                    }
                    entry<SearchKey> {
                        SearchScreen(
                            onBackClicked = { topLevelBackStack.removeLast() },
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
                    entry<AccountKey>(
                        // Compact: a full-screen settings-style page that slides in from the left,
                        // matching the "there will be more options here" brief. Expanded: centered
                        // dialog instead - DialogScene renders a real Dialog and never reads
                        // TransitionKey/PopTransitionKey, so this metadata is inert on that path.
                        metadata =
                            if (windowSize.isCompact()) {
                                NavDisplay.transitionSpec {
                                    slideInHorizontally(initialOffsetX = { -it }) togetherWith
                                        slideOutHorizontally(targetOffsetX = { it })
                                } +
                                    NavDisplay.popTransitionSpec {
                                        slideInHorizontally(initialOffsetX = { it }) togetherWith
                                            slideOutHorizontally(targetOffsetX = { -it })
                                    }
                            } else {
                                DialogSceneStrategy.dialog()
                            },
                    ) {
                        AccountScreen(
                            isDialogPresentation = !windowSize.isCompact(),
                            onBackClicked = { topLevelBackStack.removeLast() },
                            webAuthLauncher = webAuthLauncher,
                            authRepository = authRepository,
                        )
                    }
                    entry<ListDetailKey> { key ->
                        session?.let { activeSession ->
                            ListDetailScreen(
                                listId = key.listId,
                                sessionId = activeSession.sessionId,
                                onBackClicked = { topLevelBackStack.removeLast() },
                                onMovieClicked = { movieId ->
                                    topLevelBackStack.add(MovieDetailKey(movieId))
                                },
                            )
                        }
                    }
                    entry<MovieDetailKey> { key ->
                        MovieDetailScreen(
                            movieId = key.movieId,
                            windowSize = windowSize,
                            authRepository = authRepository,
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
                            authRepository = authRepository,
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
        SessionExpiredDialog(
            isVisible = showSessionExpiredDialog.value,
            onSignInClick = {
                showSessionExpiredDialog.value = false
                topLevelBackStack.switchTopLevel(MyFavKey)
            },
            onDismiss = {
                showSessionExpiredDialog.value = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    windowSize: WindowSize,
    session: UserSession?,
    onSearchClicked: () -> Unit,
    onAccountClicked: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        scrollBehavior = scrollBehavior,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
        title = {
            // SearchBox already reserves its own end padding (for the pill's rounded edge), and
            // the avatar action next to it adds more breathing room on its own - an extra end
            // padding here on top of both left a visibly oversized gap before the avatar.
            Box(
                modifier = Modifier.fillMaxWidth(),
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
                    onClick = onSearchClicked,
                )
            }
        },
        actions = {
            AccountAvatarButton(session = session, onClick = onAccountClicked)
        },
    )
}

internal expect fun openUrl(url: String?)
