package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.BackdropImage

@Composable
fun MovieImagesSection(images: List<BackdropImage>) {
    if (images.isNotEmpty()) {
        val lazyRowState = rememberLazyListState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            SectionHeaderWithScrollHint(
                title = "Photos",
                listSize = images.size.coerceAtMost(10),
                lazyRowState = lazyRowState,
                scrollStep = 2
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                state = lazyRowState,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(images.take(10)) { image ->
                    val imageUrl = "https://image.tmdb.org/t/p/w500${image.filePath}"
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .aspectRatio(16 / 9f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val painter = rememberAsyncImagePainter(model = imageUrl)
                        Image(
                            painter = painter,
                            contentDescription = "Movie backdrop",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
