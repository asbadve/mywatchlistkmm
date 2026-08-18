package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.ui.PillTabRow
import com.ajinkyabadve.kmmmywatchlist.core.ui.collapsingHeader
import com.ajinkyabadve.kmmmywatchlist.core.ui.rememberCollapsibleBarState
import com.ajinkyabadve.kmmmywatchlist.features.discover.screen.DiscoverFilterDialog
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.MovieRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.DiscoverMovieScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.DiscoverMovieTab
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.MovieListScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.category.screenContent
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.discover_filter_button_label
import mywatchlist.composeapp.generated.resources.discover_filters_content_description
import org.jetbrains.compose.resources.stringResource

sealed class MovieTab(
    val title: String,
) {
    data object NowPlaying : MovieTab("Now Playing")

    data object Upcoming : MovieTab("Upcoming")

    data object Popular : MovieTab("Popular")

    data object TopRated : MovieTab("Top Rated")

    data object Discover : MovieTab("Discover")
}

/**
 * A composable that displays movie tabs and the content for the selected tab.
 */
@Composable
fun MovieScreenTabs(
    modifier: Modifier = Modifier,
    onMovieSelected: (movieId: Long) -> Unit,
    // Test-only seam: lets UI tests inject a fake repository so each sub-tab's ScreenModel can be
    // verified to load lazily (on first selection) and cache (no refetch on re-selection) without
    // hitting real network. This overrides only the *repository*, not the ScreenModel itself -
    // construction still happens inside MovieListTab's `viewModel(key = ...) { }` factory, which
    // Compose only invokes once that sub-tab's `when` branch is actually selected. Passing a
    // pre-built ScreenModel instead would defeat the point: Kotlin evaluates constructor-call
    // arguments eagerly, so all four would construct (and fire their init{} load) up front,
    // regardless of which tab is selected. Production call sites never pass these.
    nowPlayingRepository: MovieRepository? = null,
    upcomingRepository: MovieRepository? = null,
    popularRepository: MovieRepository? = null,
    topRatedRepository: MovieRepository? = null,
    // Same test-only seam as the repository params above, for the Discover sub-tab's ScreenModel.
    discoverScreenModel: DiscoverMovieScreenModel? = null,
) {
    val tabs =
        remember {
            listOf(
                MovieTab.NowPlaying,
                MovieTab.Upcoming,
                MovieTab.Popular,
                MovieTab.TopRated,
                MovieTab.Discover,
            )
        }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    val nowPlayingGridState = rememberLazyGridState()
    val upcomingGridState = rememberLazyGridState()
    val popularGridState = rememberLazyGridState()
    val topRatedGridState = rememberLazyGridState()
    val discoverGridState = rememberLazyGridState()
    var showFilterDialog by remember { mutableStateOf(false) }

    // Only resolved (and only constructed, via the viewModel(key = ...) factory) once Discover is
    // actually selected - same lazy seam MovieListTab's four sub-tabs already use, so switching to
    // Discover doesn't eagerly fire its network call before the user ever picks that tab. The FAB
    // below reads this too, so the count badge and the dialog it opens always agree with what the
    // grid is showing - there's only one instance of this ScreenModel for the whole tab row.
    val discoverViewModel: DiscoverMovieScreenModel? =
        if (tabs[selectedTabIndex] == MovieTab.Discover) {
            discoverScreenModel ?: viewModel(key = "DiscoverMovieScreenModel") { DiscoverMovieScreenModel() }
        } else {
            null
        }

    // The category tabs leave with the app's search bar rather than staying pinned, so the grid
    // gets the whole screen on the way down. collapsingHeader shrinks the row's reported height,
    // so the grid rises into the space instead of a gap opening above it.
    val tabRowState = rememberCollapsibleBarState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().nestedScroll(tabRowState.nestedScrollConnection)) {
            PillTabRow(
                tabs = tabs.map { it.title },
                selectedIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                modifier = Modifier.fillMaxWidth().collapsingHeader(tabRowState),
            )
            when (tabs[selectedTabIndex]) {
                MovieTab.NowPlaying ->
                    MovieListTab(MoviesConstant.NOW_PLAYING_API_PATH, nowPlayingGridState, onMovieSelected, nowPlayingRepository)
                MovieTab.Upcoming ->
                    MovieListTab(MoviesConstant.UPCOMING_API_PATH, upcomingGridState, onMovieSelected, upcomingRepository)
                MovieTab.Popular ->
                    MovieListTab(MoviesConstant.POPULAR_API_PATH, popularGridState, onMovieSelected, popularRepository)
                MovieTab.TopRated ->
                    MovieListTab(MoviesConstant.TOP_RATED_API_PATH, topRatedGridState, onMovieSelected, topRatedRepository)
                MovieTab.Discover ->
                    DiscoverMovieTab(lazyGridState = discoverGridState, onMovieSelected = onMovieSelected, screenModel = discoverViewModel)
            }
        }

        if (discoverViewModel != null) {
            val activeCount = discoverViewModel.filters.genreIds.size + discoverViewModel.filters.keywords.size
            // Rides the same collapse fraction the tab row above already tracks from this grid's
            // scroll (tabRowState.nestedScrollConnection), so the FAB slides down and fades out in
            // step with it instead of sitting fixed over the content while scrolling.
            var fabHeightPx by remember { mutableStateOf(0) }
            BadgedBox(
                badge = { if (activeCount > 0) Badge { Text(activeCount.toString()) } },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .onSizeChanged { fabHeightPx = it.height }
                        .offset { IntOffset(x = 0, y = (fabHeightPx * tabRowState.collapsedFraction).toInt()) }
                        .alpha(1f - tabRowState.collapsedFraction),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showFilterDialog = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(Res.string.discover_filters_content_description),
                        )
                    },
                    text = { Text(stringResource(Res.string.discover_filter_button_label)) },
                )
            }
        }
    }

    if (showFilterDialog && discoverViewModel != null) {
        DiscoverFilterDialog(
            initialFilters = discoverViewModel.filters,
            genres = discoverViewModel.genres,
            sortOptions = DiscoverConstant.MOVIE_SORT_OPTIONS,
            onDismiss = { showFilterDialog = false },
            onApply = { newFilters ->
                discoverViewModel.applyFilters(newFilters)
                showFilterDialog = false
            },
        )
    }
}

@Composable
fun MovieListTab(
    fetchType: String,
    lazyGridState: LazyGridState,
    onMovieSelected: (movieId: Long) -> Unit,
    movieRepository: MovieRepository? = null,
) {
    val viewModel =
        viewModel(key = "MovieListScreenModel:$fetchType") {
            if (movieRepository != null) MovieListScreenModel(fetchType, movieRepository) else MovieListScreenModel(fetchType)
        }
    screenContent(viewModel = viewModel, lazyColumnListState = lazyGridState, onMovieSelected = onMovieSelected)
}
