package com.ajinkyabadve.kmmmywatchlist.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_favorite_24
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import mywatchlist.composeapp.generated.resources.baseline_person_24
import mywatchlist.composeapp.generated.resources.baseline_tv_24
import mywatchlist.composeapp.generated.resources.trending_up_24
import org.jetbrains.compose.resources.DrawableResource

/**
 * Every destination in the app. `@Serializable` - and sealed, so the compiler generates the
 * polymorphic serializer without reflection or a `SerializersModule` - because the back stack is
 * saved across Activity recreation and process death (see [rememberTopLevelBackStack]). A new key
 * must be `@Serializable` too, or saving the back stack fails at runtime.
 */
@Serializable
sealed interface AppKey : NavKey

@Serializable
data object TrendingKey : AppKey

@Serializable
data object MoviesKey : AppKey

@Serializable
data object TvShowsKey : AppKey

@Serializable
data object PersonKey : AppKey

@Serializable
data object MyFavKey : AppKey

@Serializable
data object SearchKey : AppKey

@Serializable
data object AccountKey : AppKey

@Serializable
data class MovieDetailKey(
    val movieId: Long,
) : AppKey

@Serializable
data class CollectionDetailKey(
    val collectionId: Long,
) : AppKey

@Serializable
data class TvDetailKey(
    val tvShowId: Long,
) : AppKey

@Serializable
data class PersonDetailKey(
    val personId: Long,
) : AppKey

@Serializable
data class AllSeasonsKey(
    val tvShowId: Long,
) : AppKey

@Serializable
data class EpisodeListKey(
    val tvShowId: Long,
    val seasonNumber: Int,
) : AppKey

@Serializable
data class EpisodeDetailKey(
    val tvShowId: Long,
    val seasonNumber: Int,
    val episodeNumber: Int,
) : AppKey

@Serializable
data class ListDetailKey(
    val listId: Long,
) : AppKey

/**
 * True for the drill-down destinations that open full-screen and bring their own top bar, as
 * opposed to the top-level browse tabs and search. These are the screens that suppress the app's
 * own top bar and collapse the bottom nav bar on scroll, so the two behaviours cannot drift apart.
 */
fun AppKey?.isDetailKey(): Boolean =
    when (this) {
        is MovieDetailKey,
        is CollectionDetailKey,
        is TvDetailKey,
        is PersonDetailKey,
        is AllSeasonsKey,
        is EpisodeListKey,
        is EpisodeDetailKey,
        is ListDetailKey,
        -> true

        else -> false
    }

val bottomNavItems =
    listOf(
        BottomNavItem(TrendingKey, "Trending", Res.drawable.trending_up_24),
        BottomNavItem(MoviesKey, "Movies", Res.drawable.baseline_movie_24),
        BottomNavItem(TvShowsKey, "Tv shows", Res.drawable.baseline_tv_24),
        BottomNavItem(PersonKey, "Person", Res.drawable.baseline_person_24),
        BottomNavItem(MyFavKey, "My Fav", Res.drawable.baseline_favorite_24),
    )

sealed class MovieScreenTab(
    val route: String,
    val label: String,
    val icon: DrawableResource?,
) {
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
