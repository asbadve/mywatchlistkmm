import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_favorite_24
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import mywatchlist.composeapp.generated.resources.baseline_person_24
import mywatchlist.composeapp.generated.resources.baseline_tv_24
import mywatchlist.composeapp.generated.resources.trending_up_24
import org.jetbrains.compose.resources.DrawableResource

sealed class HomepageScreen(val route: String, val label: String, val icon: DrawableResource) {
    object Trending : HomepageScreen("trending_homepage", "Trending", Res.drawable.trending_up_24)
    object Movies : HomepageScreen("movies_homepage", "Movies", Res.drawable.baseline_movie_24)
    object Tvshows : HomepageScreen("tv_shows_homepage", "Tv shows", Res.drawable.baseline_tv_24)
    object Person : HomepageScreen("person_homepage", "Person", Res.drawable.baseline_person_24)
    object MyFav : HomepageScreen("my_fav_homepage", "My Fav", Res.drawable.baseline_favorite_24)
}

val bottomNavItems = listOf(
    HomepageScreen.Trending,
    HomepageScreen.Movies,
    HomepageScreen.Tvshows,
    HomepageScreen.Person,
    HomepageScreen.MyFav
)

sealed class MovieScreenTab(val route: String, val label: String, val icon: DrawableResource?) {
    object NowPlaying : MovieScreenTab("now_playing_movie_tab", "Now Playing", null)
    object Upcoming : MovieScreenTab("upcoming_movie_tab", "Upcoming", null)
    object Popular : MovieScreenTab("popular_movie_tab", "Popular", null)
    object TopRated : MovieScreenTab("top_rated_movie_tab", "Top Rated", null)
}

val movieScreenTab = listOf(
    MovieScreenTab.NowPlaying,
    MovieScreenTab.Upcoming,
    MovieScreenTab.Popular,
    MovieScreenTab.TopRated
)

sealed class Destination(val route: String) {
    object MovieDetail : Destination("movie_detail/{movieId}") {
        const val ARG_MOVIE_ID = "movieId"
        fun createRoute(movieId: Long) = "movie_detail/$movieId"
    }
    object TvDetail : Destination("tv_detail/{tvShowId}") {
        const val ARG_TV_SHOW_ID = "tvShowId"
        fun createRoute(tvShowId: Long) = "tv_detail/$tvShowId"
    }
    object AllSeasons : Destination("all_seasons/{tvShowId}") {
        const val ARG_TV_SHOW_ID = "tvShowId"
        fun createRoute(tvShowId: Long) = "all_seasons/$tvShowId"
    }
}
