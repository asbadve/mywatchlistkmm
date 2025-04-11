package com.ajinkyabadve.kmmmywatchlist.homepage.model

import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.FavTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.PersonTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TrendingTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TvShowsTab
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_favorite_24
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import mywatchlist.composeapp.generated.resources.baseline_person_24
import mywatchlist.composeapp.generated.resources.baseline_trending_24
import mywatchlist.composeapp.generated.resources.baseline_tv_24
import org.jetbrains.compose.resources.DrawableResource

data class HomeNavigation(
    val drawableResource: DrawableResource,
    val label: String,
    val selected: Boolean,
    val iconContentDescription: String,
)

object TabNavigation {
    fun getNavigation(): List<HomeNavigation> {
        return listOf(
            HomeNavigation(
                drawableResource = Res.drawable.baseline_trending_24,
                label = AppTabs.TRENDING,
                selected = false,
                iconContentDescription = AppTabs.TRENDING,
            ),
            HomeNavigation(
                drawableResource = Res.drawable.baseline_movie_24,
                label = AppTabs.MOVIES,
                selected = false,
                iconContentDescription = AppTabs.MOVIES,
            ),
            HomeNavigation(
                label = AppTabs.TV_SHOWS,
                drawableResource = Res.drawable.baseline_tv_24,
                selected = false,
                iconContentDescription = AppTabs.TV_SHOWS,
            ),
            HomeNavigation(
                drawableResource = Res.drawable.baseline_person_24,
                label = AppTabs.PERSON,
                selected = false,
                iconContentDescription = AppTabs.PERSON,
            ),
            HomeNavigation(
                drawableResource = Res.drawable.baseline_favorite_24,
                label = AppTabs.FAV,
                selected = false,
                iconContentDescription = AppTabs.FAV,
            ),
        )
    }
}

object AppTabs {
    const val MOVIES = "Movies"
    const val TV_SHOWS = "Tv shows"
    const val PERSON = "Person"
    const val TRENDING = "Trending"
    const val FAV = "My Fav"

    fun getTabs() = listOf(TrendingTab, MoviesTab, TvShowsTab, PersonTab, FavTab)
}
