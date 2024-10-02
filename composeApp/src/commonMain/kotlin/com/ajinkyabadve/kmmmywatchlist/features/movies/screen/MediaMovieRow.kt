package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.design.movie.MediaCard
import com.seiko.imageloader.rememberImagePainter

@Composable
fun mediaMovieRow(
    imageUrl: String?,
    title: String,
    modifier: Modifier,
) {
    var painter: Painter? = null
    imageUrl?.let {
        painter = rememberImagePainter(url = imageUrl, filterQuality = FilterQuality.Medium)
    }
    Box(
        modifier.padding(8.dp),
    ) {
        painter?.let {
            MediaCard(
                modifier,
                title,
                it,
            )
        }
    }
}
