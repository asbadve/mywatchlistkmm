package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.mediaMovieRow

@Composable
fun RecommendationsSection(
    recommendations: List<Movie>,
    onMovieClicked: (Long) -> Unit
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
                items(recommendations.take(10)) { movie ->
                    val density = androidx.compose.ui.platform.LocalDensity.current.density
                    val imageUrl = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.resolve(
                        path = movie.posterPath,
                        type = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.POSTER,
                        targetWidthDp = 140,
                        density = density
                    )
                    Box(modifier = Modifier.width(140.dp)) {
                        mediaMovieRow(
                            imageUrl = imageUrl,
                            title = movie.title,
                            modifier = Modifier,
                            onClick = { onMovieClicked(movie.id.toLong()) }
                        )
                    }
                }
            }
        }
    }
}
