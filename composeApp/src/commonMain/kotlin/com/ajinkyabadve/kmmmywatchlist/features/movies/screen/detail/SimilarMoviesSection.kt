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
fun SimilarMoviesSection(
    similarMovies: List<Movie>,
    onMovieClicked: (Long) -> Unit
) {
    if (similarMovies.isNotEmpty()) {
        val lazyRowState = rememberLazyListState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            SectionHeaderWithScrollHint(
                title = "Similar Movies",
                listSize = similarMovies.size.coerceAtMost(10),
                lazyRowState = lazyRowState,
                scrollStep = 2
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                state = lazyRowState,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(similarMovies.take(10)) { movie ->
                    Box(modifier = Modifier.width(140.dp)) {
                        mediaMovieRow(
                            imageUrl = movie.posterPath?.let { MoviesConstant.IMAGE_BASE_URL + it },
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
