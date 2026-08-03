package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.TvShowScreenTabs

@Composable
fun TvShowsScreenTab(
    modifier: Modifier = Modifier,
    onTvShowSelected: (tvShowId: Long) -> Unit = {},
) {
    TvShowScreenTabs(
        modifier = modifier,
        onTvShowSelected = onTvShowSelected,
    )
}
