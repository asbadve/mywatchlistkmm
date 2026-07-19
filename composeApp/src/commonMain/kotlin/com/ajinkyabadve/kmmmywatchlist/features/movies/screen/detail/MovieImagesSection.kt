package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import com.ajinkyabadve.kmmmywatchlist.design.util.FullscreenMediaGallery
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.BackdropImage
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver

@Composable
fun MovieImagesSection(
    images: List<BackdropImage>,
    title: String,
    imageType: ImageConfigResolver.ImageType,
    onShowGallery: (images: List<String>, index: Int) -> Unit
) {
    if (images.isNotEmpty()) {
        val lazyRowState = rememberLazyListState()
        val photoList = images.take(10)

        // Configure card layout specifics based on image category type: posters and person
        // profiles are portrait (2:3), backdrops and episode stills are landscape (16:9)
        val isPortrait = imageType == ImageConfigResolver.ImageType.POSTER ||
            imageType == ImageConfigResolver.ImageType.PROFILE
        val cardWidth = if (isPortrait) 120.dp else 200.dp
        val aspectRatio = if (isPortrait) 2 / 3f else 16 / 9f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            SectionHeaderWithScrollHint(
                title = title,
                listSize = photoList.size,
                lazyRowState = lazyRowState,
                scrollStep = 2
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                state = lazyRowState,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(photoList) { index, image ->
                    val density = androidx.compose.ui.platform.LocalDensity.current.density
                    val imageUrl = ImageConfigResolver.resolve(
                        path = image.filePath,
                        type = imageType,
                        targetWidthDp = if (isPortrait) 150 else 200,
                        density = density
                    )
                    Card(
                        modifier = Modifier
                            .width(cardWidth)
                            .aspectRatio(aspectRatio)
                            .clickable {
                                val highResUrls = photoList.map { img ->
                                    ImageConfigResolver.resolve(
                                        path = img.filePath,
                                        type = imageType,
                                        targetWidthDp = 1920,
                                        density = density
                                    ) ?: ""
                                }
                                onShowGallery(highResUrls, index)
                            },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val painter = rememberAsyncImagePainter(model = imageUrl)
                        Image(
                            painter = painter,
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
