package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.ui.collapsingHeader
import com.ajinkyabadve.kmmmywatchlist.core.ui.rememberCollapsibleBarState
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.repository.TvRepository
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category.TvListScreenModel
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category.tvShowScreenContent

sealed class TvTab(
    val title: String,
) {
    data object AiringToday : TvTab("Airing Today")

    data object OnTheAir : TvTab("On The Air")

    data object Popular : TvTab("Popular")

    data object TopRated : TvTab("Top Rated")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvShowScreenTabs(
    modifier: Modifier = Modifier,
    onTvShowSelected: (tvShowId: Long) -> Unit,
    // Test-only seam: lets UI tests inject a fake repository so each sub-tab's ScreenModel can be
    // verified to load lazily (on first selection) and cache (no refetch on re-selection) without
    // hitting real network. This overrides only the *repository*, not the ScreenModel itself -
    // construction still happens inside TvListTab's `viewModel(key = ...) { }` factory, which
    // Compose only invokes once that sub-tab's `when` branch is actually selected. Passing a
    // pre-built ScreenModel instead would defeat the point: Kotlin evaluates constructor-call
    // arguments eagerly, so all four would construct (and fire their init{} load) up front,
    // regardless of which tab is selected. Production call sites never pass these.
    airingTodayRepository: TvRepository? = null,
    onTheAirRepository: TvRepository? = null,
    popularRepository: TvRepository? = null,
    topRatedRepository: TvRepository? = null,
) {
    val tabs =
        remember {
            listOf(
                TvTab.AiringToday,
                TvTab.OnTheAir,
                TvTab.Popular,
                TvTab.TopRated,
            )
        }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    val airingTodayGridState = rememberLazyGridState()
    val onTheAirGridState = rememberLazyGridState()
    val popularGridState = rememberLazyGridState()
    val topRatedGridState = rememberLazyGridState()

    // The category tabs leave with the app's search bar rather than staying pinned, so the grid
    // gets the whole screen on the way down. collapsingHeader shrinks the row's reported height,
    // so the grid rises into the space instead of a gap opening above it.
    val tabRowState = rememberCollapsibleBarState()

    Column(modifier = modifier.fillMaxWidth().nestedScroll(tabRowState.nestedScrollConnection)) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().collapsingHeader(tabRowState),
            contentAlignment = Alignment.Center,
        ) {
            val tabsWidth = 400.dp
            val calculatedPadding = (maxWidth - tabsWidth) / 2
            val edgePadding = if (calculatedPadding > 0.dp) calculatedPadding else 0.dp

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = edgePadding,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                indicator = {
                    Box(
                        modifier =
                            Modifier
                                .tabIndicatorOffset(selectedTabIndex)
                                .padding(horizontal = 24.dp)
                                .height(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                ),
                    )
                },
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab.title) },
                    )
                }
            }
        }
        when (tabs[selectedTabIndex]) {
            TvTab.AiringToday ->
                TvListTab(TvShowsConstant.AIRING_TODAY_API_PATH, airingTodayGridState, onTvShowSelected, airingTodayRepository)
            TvTab.OnTheAir ->
                TvListTab(TvShowsConstant.ON_THE_AIR_API_PATH, onTheAirGridState, onTvShowSelected, onTheAirRepository)
            TvTab.Popular ->
                TvListTab(TvShowsConstant.POPULAR_API_PATH, popularGridState, onTvShowSelected, popularRepository)
            TvTab.TopRated ->
                TvListTab(TvShowsConstant.TOP_RATED_API_PATH, topRatedGridState, onTvShowSelected, topRatedRepository)
        }
    }
}

@Composable
fun TvListTab(
    fetchType: String,
    lazyGridState: LazyGridState,
    onTvShowSelected: (tvShowId: Long) -> Unit,
    tvRepository: TvRepository? = null,
) {
    val viewModel =
        viewModel(key = "TvListScreenModel:$fetchType") {
            if (tvRepository != null) TvListScreenModel(fetchType, tvRepository) else TvListScreenModel(fetchType)
        }
    tvShowScreenContent(viewModel = viewModel, lazyColumnListState = lazyGridState, onTvShowSelected = onTvShowSelected)
}
