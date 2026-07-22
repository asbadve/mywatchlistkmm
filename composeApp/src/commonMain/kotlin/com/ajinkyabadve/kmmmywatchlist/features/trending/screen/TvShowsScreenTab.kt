package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowScreenTabs
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.category.TvListScreenModel

@Composable
fun TvShowsScreenTab(
    modifier: Modifier = Modifier,
    airingTodayViewModel: TvListScreenModel,
    onTheAirViewModel: TvListScreenModel,
    popularViewModel: TvListScreenModel,
    topRatedViewModel: TvListScreenModel,
    onTvShowSelected: (tvShowId: Long) -> Unit = {},
) {
    TvShowScreenTabs(
        modifier = modifier,
        airingTodayViewModel = airingTodayViewModel,
        onTheAirViewModel = onTheAirViewModel,
        popularViewModel = popularViewModel,
        topRatedViewModel = topRatedViewModel,
        onTvShowSelected = onTvShowSelected,
    )
}
