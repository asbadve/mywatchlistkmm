package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController

sealed class MovieTab(val title: String) {
    data object NowPlaying : MovieTab("Now Playing")
    data object Upcoming : MovieTab("Upcoming")
    data object Popular : MovieTab("Popular")
    data object TopRated : MovieTab("Top Rated")
}

/**
 * A composable that displays movie tabs and the content for the selected tab.
 *
 * @param onMovieSelected A callback function to navigate to a movie detail screen.
 *                        This is the idiomatic KMM way to handle navigation.
 *                        In your containing screen (e.g., a NavHost destination),
 *                        you will connect this callback to your actual NavController.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieScreenTabs(
    modifier: Modifier = Modifier,
    onMovieSelected: (movieId: Long) -> Unit
) {
    val tabs = remember {
        listOf(
            MovieTab.NowPlaying,
            MovieTab.Upcoming,
            MovieTab.Popular,
            MovieTab.TopRated
        )
    }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val internalNavController = rememberNavController()
    Column(modifier = modifier.fillMaxWidth()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
//            contentColor = Color.Transparent,
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(tab.title) }
                )
            }
        }
        when (tabs[selectedTabIndex]) {
            MovieTab.NowPlaying -> NowPlayingTab(onMovieSelected)
            MovieTab.Upcoming -> UpcomingTab(onMovieSelected)
            MovieTab.Popular -> PopularTab(onMovieSelected)
            MovieTab.TopRated -> TopRatedTab(onMovieSelected)
        }
    }


//    Column(modifier = modifier.fillMaxSize()) {
//        ScrollableTabRow( // Changed TabRow to ScrollableTabRow
//            selectedTabIndex = selectedTabIndex,
//            indicator = customIndicator,
//            // Google Play Store tabs do not have a divider. We remove it by passing an empty composable.
//            divider = {}
//        ) {
//            tabs.forEachIndexed { index, tab ->
//                Tab(
//                    selected = selectedTabIndex == index,
//                    onClick = { selectedTabIndex = index },
//                    text = { Text(tab.title) }
//                )
//            }
//        }
//
//        when (tabs[selectedTabIndex]) {
//            MovieTab.NowPlaying -> NowPlayingTab(onMovieSelected)
//            MovieTab.Upcoming -> UpcomingTab(onMovieSelected)
//            MovieTab.Popular -> PopularTab(onMovieSelected)
//            MovieTab.TopRated -> TopRatedTab(onMovieSelected)
//        }
//    }
}

@Composable
fun NowPlayingTab(onMovieSelected: (movieId: Long) -> Unit) {
    // TODO: Implement Now Playing content. When a movie item is clicked, call onMovieSelected(movieId).
    Text("Now Playing Content")
}

@Composable
fun UpcomingTab(onMovieSelected: (movieId: Long) -> Unit) {
    // TODO: Implement Upcoming content. When a movie item is clicked, call onMovieSelected(movieId).
    Text("Upcoming Content")
}

@Composable
fun PopularTab(onMovieSelected: (movieId: Long) -> Unit) {
    // TODO: Implement Popular content. When a movie item is clicked, call onMovieSelected(movieId).
    Text("Popular Content")
}

@Composable
fun TopRatedTab(onMovieSelected: (movieId: Long) -> Unit) {
    // TODO: Implement Top Rated content. When a movie item is clicked, call onMovieSelected(movieId).
    Text("Top Rated Content")
}