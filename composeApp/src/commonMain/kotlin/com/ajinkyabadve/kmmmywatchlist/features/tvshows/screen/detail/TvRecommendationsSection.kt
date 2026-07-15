package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.SectionHeaderWithScrollHint
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.Tv
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.mediaTvShowRow

@Composable
fun TvRecommendationsSection(
    recommendations: List<Tv>,
    onTvShowClicked: (Long) -> Unit
) {
    if (recommendations.isNotEmpty()) {
        val lazyRowState = rememberLazyListState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            SectionHeaderWithScrollHint(
                title = "Recommendations",
                listSize = recommendations.size.coerceAtMost(10),
                lazyRowState = lazyRowState,
                scrollStep = 2
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                state = lazyRowState,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(recommendations.take(10)) { tvShow ->
                    val density = androidx.compose.ui.platform.LocalDensity.current.density
                    val imageUrl = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.resolve(
                        path = tvShow.posterPath,
                        type = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.POSTER,
                        targetWidthDp = 140,
                        density = density
                    )
                    Box(modifier = Modifier.width(140.dp)) {
                        mediaTvShowRow(
                            imageUrl = imageUrl,
                            title = tvShow.title,
                            modifier = Modifier,
                            onClick = { onTvShowClicked(tvShow.id.toLong()) }
                        )
                    }
                }
            }
        }
    }
}
