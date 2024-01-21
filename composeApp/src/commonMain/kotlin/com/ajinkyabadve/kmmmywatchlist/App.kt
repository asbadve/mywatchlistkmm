@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.ajinkyabadve.kmmmywatchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabDisposable
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.ajinkyabadve.kmmmywatchlist.design.searchbox.SearchBox
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TvShowsTab
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_surface
import org.jetbrains.compose.resources.painterResource

@ExperimentalMaterial3Api
@Composable
internal fun App(windowSize: MoviesTab.WindowSize) = AppTheme {
    content()
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@ExperimentalMaterial3Api
@Composable
fun content() {
    val windowSizeClass = calculateWindowSizeClass()
    val windowSize =
        MoviesTab.WindowSize.basedOnWindowSizeClass(windowSizeClass.widthSizeClass.toString())

    TabNavigator(MoviesTab, tabDisposable = {
        TabDisposable(
            navigator = it, tabs = listOf(MoviesTab, TvShowsTab)
        )
    }) { tabNavigator ->
        var selectedChip by remember { mutableStateOf(0) }
        val chipItemList = listOf(
            MoviesTab.Tabs.NOW_PLAYING_MOVIES,
            MoviesTab.Tabs.UPCOMING_MOVIES,
            MoviesTab.Tabs.POPULAR_MOVIES,
            MoviesTab.Tabs.TOP_RATED_MOVIES
        )
        Scaffold(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars), topBar = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (windowSize == MoviesTab.WindowSize.COMPACT) {
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
                    myWatchListScrollableChips(selectedTab = selectedChip,
                        tabItemList = chipItemList,
                        onClick = { index ->
                            selectedChip = index
                        })
                }
            }
        }, bottomBar = {
            if (windowSize == MoviesTab.WindowSize.COMPACT || windowSize == MoviesTab.WindowSize.MEDIUM) {
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
                if (windowSize == MoviesTab.WindowSize.EXPANDED) {

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
                    if (windowSize == MoviesTab.WindowSize.EXPANDED || windowSize == MoviesTab.WindowSize.MEDIUM) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SearchBox(
                                hint = "Search Movies, Tv shows, Person",
                                modifier = Modifier.wrapContentHeight()
                            ) { }
                            myWatchListScrollableChips(selectedTab = selectedChip,
                                tabItemList = chipItemList,
                                onClick = { index ->
                                    selectedChip = index
                                })
                        }

                    }
                    CurrentTab()
                }
            }
        }
    }
}

@Composable
private fun myWatchListScrollableChips(
    selectedTab: Int,
    tabItemList: List<String>,
    onClick: (index: Int) -> Unit,
) {

    val selectionColor: @Composable (Boolean) -> Color = { selection ->
        if (selection) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    }
    LazyRow(
        modifier = Modifier.background(md_theme_dark_surface),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(tabItemList) { index, item ->
            ElevatedAssistChip(
                onClick = { onClick(index) },
                label = { Text(item) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = selectionColor(
                        selectedTab == index
                    )
                ),
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

internal expect fun openUrl(url: String?)


