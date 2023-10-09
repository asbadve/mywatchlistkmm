package com.ajinkyabadve.kmmmywatchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChipDefaults.assistChipColors
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.Tabs.DISCOVER
import com.ajinkyabadve.kmmmywatchlist.Tabs.FAV
import com.ajinkyabadve.kmmmywatchlist.Tabs.IMAGE_BASE_URL
import com.ajinkyabadve.kmmmywatchlist.Tabs.MOVIES
import com.ajinkyabadve.kmmmywatchlist.Tabs.NOW_PLAYING_MOVIES
import com.ajinkyabadve.kmmmywatchlist.Tabs.PERSON
import com.ajinkyabadve.kmmmywatchlist.Tabs.POPULAR_MOVIES
import com.ajinkyabadve.kmmmywatchlist.Tabs.TOP_RATED_MOVIES
import com.ajinkyabadve.kmmmywatchlist.Tabs.TV_SHOWS
import com.ajinkyabadve.kmmmywatchlist.Tabs.UPCOMING_MOVIES
import com.ajinkyabadve.kmmmywatchlist.design.MovieCard
import com.ajinkyabadve.kmmmywatchlist.design.searchbox.SearchBox
import com.ajinkyabadve.kmmmywatchlist.features.nowplaying.MovieListScreenState
import com.ajinkyabadve.kmmmywatchlist.features.nowplaying.NowPlayingMoviesViewModel
import com.ajinkyabadve.kmmmywatchlist.imageloader.generateImageLoader
import com.ajinkyabadve.kmmmywatchlist.theme.AppTheme
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_surface
import com.seiko.imageloader.LocalImageLoader
import com.seiko.imageloader.rememberImagePainter
import org.jetbrains.compose.resources.painterResource

@ExperimentalMaterial3Api
@Composable
internal fun App(windowSize: WindowSize) = AppTheme {
    val viewModel = NowPlayingMoviesViewModel()
    val state = viewModel.state.collectAsState()
    CompositionLocalProvider(
        LocalImageLoader provides remember { generateImageLoader() },
    ) {
        var selectedNavItem by remember { mutableStateOf(0) }
        var selectedTab by remember { mutableStateOf(0) }
        val navItemList = listOf(
            NavItem(MOVIES, painterResource("baseline_movie_24.xml")),
            NavItem(TV_SHOWS, painterResource("baseline_tv_24.xml")),
            NavItem(PERSON, painterResource("baseline_person_24.xml")),
            NavItem(DISCOVER, painterResource("baseline_discover_24.xml")),
            NavItem(FAV, painterResource("baseline_favorite_24.xml"))
        )
        val tabItemList =
            listOf(NOW_PLAYING_MOVIES, UPCOMING_MOVIES, POPULAR_MOVIES, TOP_RATED_MOVIES)
        Scaffold(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            topBar = {
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
                                        onClick = {
                                        },
                                    )
                                }
                            },
                        )
                        myWatchListScrollableChips(
                            selectedTab = selectedTab,
                            tabItemList = tabItemList,
                            onClick = { index ->
                                selectedTab = index
                            }
                        )
                    }
                }
            },
            bottomBar = {
                if (windowSize == WindowSize.COMPACT || windowSize == WindowSize.MEDIUM) {
                    NavigationBar {
                        navItemList.forEachIndexed { index, item ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.title,
                                    )
                                },
                                label = { Text(item.title) },
                                selected = selectedNavItem == index,
                                onClick = { selectedNavItem = index },
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(innerPadding).background(md_theme_dark_surface)
            ) {
                if (windowSize == WindowSize.EXPANDED) {
                    NavigationRail(
                        content = {
                            Spacer(Modifier.weight(1f))
                            navItemList.forEachIndexed { index, item ->
                                NavigationRailItem(
                                    modifier = Modifier.padding(8.dp),
                                    icon = {
                                        Icon(
                                            item.icon,
                                            contentDescription = item.title,
                                        )
                                    },
                                    label = { Text(item.title) },
                                    selected = selectedNavItem == index,
                                    onClick = { selectedNavItem = index },
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    )
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
                            myWatchListScrollableChips(
                                selectedTab = selectedTab,
                                tabItemList = tabItemList,
                                onClick = { index ->
                                    selectedTab = index

                                }
                            )
                        }

                    }
                    when (val result = state.value) {
                        is MovieListScreenState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is MovieListScreenState.Success -> {
                            LazyVerticalGrid(
                                state = rememberLazyGridState(),
                                columns = GridCells.Fixed(getGridColumn(windowSize)),
                                contentPadding = PaddingValues(8.dp),
                            ) {
                                items(result.countriesList) { movie ->
                                    MovieRow(
                                        IMAGE_BASE_URL + movie.posterPath,
                                        movie.title,
                                    )
                                }
                            }
                        }

                        is MovieListScreenState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = result.message)
                            }
                        }
                    }
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
                colors = assistChipColors(containerColor = selectionColor(selectedTab == index)),
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

private fun getGridColumn(windowSize: WindowSize): Int {
    return when (windowSize) {
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

internal expect fun openUrl(url: String?)

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
