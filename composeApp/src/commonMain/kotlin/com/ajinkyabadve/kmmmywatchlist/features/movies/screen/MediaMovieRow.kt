package com.ajinkyabadve.kmmmywatchlist.features.movies.screen

import androidx.compose.foundation.layout.Row
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
) {
    var painter: Painter? = null
    imageUrl?.let {
        painter = rememberImagePainter(url = imageUrl, filterQuality = FilterQuality.Medium)
    }
    Row(Modifier.padding(8.dp)) {
        painter?.let {
            MediaCard(
                Modifier,
                title,
                it,
            )
        }
    }
}
