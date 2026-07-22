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
import androidx.compose.ui.unit.dp
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
    airingTodayViewModel: TvListScreenModel,
    onTheAirViewModel: TvListScreenModel,
    popularViewModel: TvListScreenModel,
    topRatedViewModel: TvListScreenModel,
    onTvShowSelected: (tvShowId: Long) -> Unit,
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

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
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
            TvTab.AiringToday -> TvListTab(airingTodayViewModel, airingTodayGridState, onTvShowSelected)
            TvTab.OnTheAir -> TvListTab(onTheAirViewModel, onTheAirGridState, onTvShowSelected)
            TvTab.Popular -> TvListTab(popularViewModel, popularGridState, onTvShowSelected)
            TvTab.TopRated -> TvListTab(topRatedViewModel, topRatedGridState, onTvShowSelected)
        }
    }
}

@Composable
fun TvListTab(
    viewModel: TvListScreenModel,
    lazyGridState: LazyGridState,
    onTvShowSelected: (tvShowId: Long) -> Unit,
) {
    tvShowScreenContent(viewModel = viewModel, lazyColumnListState = lazyGridState, onTvShowSelected = onTvShowSelected)
}
