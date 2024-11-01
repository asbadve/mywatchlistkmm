package com.ajinkyabadve.kmmmywatchlist.homepage.model

import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.FavTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.MoviesTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.PersonTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TrendingTab
import com.ajinkyabadve.kmmmywatchlist.homepage.tabs.TvShowsTab

data class HomeNavigation(
    val painterRes: String,
    val label: String,
    val selected: Boolean,
    val iconContentDescription: String,
)

object TabNavigation {
    fun getNavigation(): List<HomeNavigation> {
        return listOf(
            HomeNavigation(
                painterRes = "baseline_trending_24.xml",
                label = AppTabs.TRENDING,
                selected = false,
                iconContentDescription = AppTabs.TRENDING,
            ),
            HomeNavigation(
                painterRes = "baseline_movie_24.xml",
                label = AppTabs.MOVIES,
                selected = false,
                iconContentDescription = AppTabs.MOVIES,
            ),
            HomeNavigation(
                painterRes = "baseline_tv_24.xml",
                label = AppTabs.TV_SHOWS,
                selected = false,
                iconContentDescription = AppTabs.TV_SHOWS,
            ),
            HomeNavigation(
                painterRes = "baseline_person_24.xml",
                label = AppTabs.PERSON,
                selected = false,
                iconContentDescription = AppTabs.PERSON,
            ),
            HomeNavigation(
                painterRes = "baseline_favorite_24.xml",
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
