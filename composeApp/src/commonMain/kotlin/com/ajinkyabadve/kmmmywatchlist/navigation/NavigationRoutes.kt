package com.ajinkyabadve.kmmmywatchlist.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_favorite_24
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import mywatchlist.composeapp.generated.resources.baseline_person_24
import mywatchlist.composeapp.generated.resources.baseline_tv_24
import mywatchlist.composeapp.generated.resources.trending_up_24
import org.jetbrains.compose.resources.DrawableResource

sealed interface AppKey : NavKey

data object TrendingKey : AppKey
data object MoviesKey : AppKey
data object TvShowsKey : AppKey
data object PersonKey : AppKey
data object MyFavKey : AppKey

data class MovieDetailKey(val movieId: Long) : AppKey
data class CollectionDetailKey(val collectionId: Long) : AppKey
data class TvDetailKey(val tvShowId: Long) : AppKey
data class PersonDetailKey(val personId: Long) : AppKey
data class AllSeasonsKey(val tvShowId: Long) : AppKey
data class EpisodeListKey(val tvShowId: Long, val seasonNumber: Int) : AppKey
data class EpisodeDetailKey(val tvShowId: Long, val seasonNumber: Int, val episodeNumber: Int) : AppKey

data class BottomNavItem(val key: AppKey, val label: String, val icon: DrawableResource)

val bottomNavItems =
    listOf(
        BottomNavItem(TrendingKey, "Trending", Res.drawable.trending_up_24),
        BottomNavItem(MoviesKey, "Movies", Res.drawable.baseline_movie_24),
        BottomNavItem(TvShowsKey, "Tv shows", Res.drawable.baseline_tv_24),
        BottomNavItem(PersonKey, "Person", Res.drawable.baseline_person_24),
        BottomNavItem(MyFavKey, "My Fav", Res.drawable.baseline_favorite_24),
    )

sealed class MovieScreenTab(val route: String, val label: String, val icon: DrawableResource?) {
    object NowPlaying : MovieScreenTab("now_playing_movie_tab", "Now Playing", null)
    object Upcoming : MovieScreenTab("upcoming_movie_tab", "Upcoming", null)
    object Popular : MovieScreenTab("popular_movie_tab", "Popular", null)
    object TopRated : MovieScreenTab("top_rated_movie_tab", "Top Rated", null)
}

val movieScreenTab =
    listOf(
        MovieScreenTab.NowPlaying,
        MovieScreenTab.Upcoming,
        MovieScreenTab.Popular,
        MovieScreenTab.TopRated,
    )

/**
 * Owns one child back stack per bottom-nav tab (so switching tabs preserves each tab's own
 * navigation depth, matching the old Navigation 2 popUpTo/saveState/restoreState behavior) and
 * flattens them into the single list [NavDisplay] needs. Modeled on Android's own Navigation 3
 * "top level back stack" recipe for bottom navigation.
 */
class TopLevelBackStack(startKey: AppKey) {
    private val topLevelStack = ArrayDeque<AppKey>().apply { addLast(startKey) }
    private val topLevelChildStacks = mutableMapOf<AppKey, NavBackStack<AppKey>>(startKey to NavBackStack(startKey))

    var topLevelKey by mutableStateOf(startKey)
        private set

    val backStack = NavBackStack<AppKey>(startKey)

    private fun updateBackStack() {
        backStack.clear()
        backStack.addAll(topLevelStack.flatMap { topLevelChildStacks.getValue(it) })
    }

    fun switchTopLevel(key: AppKey) {
        if (topLevelChildStacks[key] == null) {
            topLevelChildStacks[key] = NavBackStack(key)
        } else {
            topLevelStack.remove(key)
        }
        topLevelStack.addLast(key)
        topLevelKey = key
        updateBackStack()
    }

    fun add(key: AppKey) {
        topLevelChildStacks.getValue(topLevelKey).add(key)
        updateBackStack()
    }

    fun removeLast() {
        if (backStack.size <= 1) return
        val childStack = topLevelChildStacks.getValue(topLevelKey)
        if (childStack.size > 1) {
            childStack.removeAt(childStack.lastIndex)
        } else {
            topLevelStack.removeLast()
            topLevelChildStacks.remove(topLevelKey)
            topLevelKey = topLevelStack.last()
        }
        updateBackStack()
    }
}
