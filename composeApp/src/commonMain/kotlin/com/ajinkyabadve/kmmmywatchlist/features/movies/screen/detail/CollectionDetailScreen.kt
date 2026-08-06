package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.core.ui.DetailTopBar
import com.ajinkyabadve.kmmmywatchlist.design.util.FullscreenMediaGallery
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import com.ajinkyabadve.kmmmywatchlist.util.ImageDownloader
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import mywatchlist.composeapp.generated.resources.featured_cast
import mywatchlist.composeapp.generated.resources.featured_crew
import mywatchlist.composeapp.generated.resources.no_overview_available
import mywatchlist.composeapp.generated.resources.section_images
import mywatchlist.composeapp.generated.resources.section_movies
import mywatchlist.composeapp.generated.resources.title_collection
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    collectionId: Long,
    windowSize: WindowSize,
    onBackClicked: () -> Unit,
    onMovieClicked: (Long) -> Unit,
    onPersonClicked: (Long) -> Unit = {},
    viewModel: CollectionDetailScreenModel =
        viewModel(key = "CollectionDetailScreenModel:$collectionId") { CollectionDetailScreenModel(collectionId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    var galleryImages by remember { mutableStateOf<List<String>?>(null) }
    var galleryInitialIndex by remember { mutableStateOf(0) }

    // Hide-on-scroll-down / reveal-on-scroll-up, matching the app's collapsing bottom nav.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DetailTopBar(
                title =
                    (uiState as? CollectionDetailState.Success)?.collection?.name
                        ?: stringResource(Res.string.title_collection),
                onBackClicked = onBackClicked,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (val state = uiState) {
                is CollectionDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is CollectionDetailState.Error -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { viewModel.loadCollectionDetails() }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }

                is CollectionDetailState.Success -> {
                    val onShowGallery: (List<String>, Int) -> Unit = { images, index ->
                        galleryImages = images
                        galleryInitialIndex = index
                    }
                    if (windowSize.isCompact()) {
                        CompactCollectionDetailContent(
                            state = state,
                            onMovieClicked = onMovieClicked,
                            onPersonClicked = onPersonClicked,
                            onShowGallery = onShowGallery,
                        )
                    } else {
                        ExpandedCollectionDetailContent(
                            state = state,
                            onMovieClicked = onMovieClicked,
                            onPersonClicked = onPersonClicked,
                            onShowGallery = onShowGallery,
                        )
                    }
                }
            }

            galleryImages?.let { images ->
                FullscreenMediaGallery(
                    images = images,
                    initialIndex = galleryInitialIndex,
                    onDismiss = { galleryImages = null },
                    onDownload = { imageUrl -> ImageDownloader.downloadAndSave(imageUrl) },
                )
            }
        }
    }
}

@Composable
private fun CompactCollectionDetailContent(
    state: CollectionDetailState.Success,
    onMovieClicked: (Long) -> Unit,
    onPersonClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    val collection = state.collection
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { CollectionHeader(collection = collection) }
        item { CollectionMoviesList(collection = collection, onMovieClicked = onMovieClicked) }
        item {
            CastSection(castList = state.featuredCast, title = stringResource(Res.string.featured_cast), onPersonClicked = onPersonClicked)
        }
        item {
            CastSection(castList = state.featuredCrew, title = stringResource(Res.string.featured_crew), onPersonClicked = onPersonClicked)
        }
        item {
            MovieImagesSection(
                images = collection.images?.backdrops ?: emptyList(),
                title = stringResource(Res.string.section_images),
                imageType = ImageConfigResolver.ImageType.BACKDROP,
                onShowGallery = onShowGallery,
            )
        }
    }
}

// Same half-and-half split the other detail screens use on non-compact widths: the collection
// identity on the left, its movies on the right.
@Composable
private fun ExpandedCollectionDetailContent(
    state: CollectionDetailState.Success,
    onMovieClicked: (Long) -> Unit,
    onPersonClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    val collection = state.collection
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { CollectionHeader(collection = collection) }
            item {
                CastSection(
                    castList = state.featuredCast,
                    title = stringResource(Res.string.featured_cast),
                    onPersonClicked = onPersonClicked,
                )
            }
            item {
                CastSection(
                    castList = state.featuredCrew,
                    title = stringResource(Res.string.featured_crew),
                    onPersonClicked = onPersonClicked,
                )
            }
            item {
                MovieImagesSection(
                    images = collection.images?.backdrops ?: emptyList(),
                    title = stringResource(Res.string.section_images),
                    imageType = ImageConfigResolver.ImageType.BACKDROP,
                    onShowGallery = onShowGallery,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp),
        ) {
            item { CollectionMoviesList(collection = collection, onMovieClicked = onMovieClicked) }
        }
    }
}

@Composable
private fun CollectionHeader(collection: CollectionDetail) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current.density
        val backdropUrl =
            ImageConfigResolver.resolve(
                path = collection.backdropPath ?: collection.posterPath,
                type = ImageConfigResolver.ImageType.BACKDROP,
                targetWidthDp = 800,
                density = density,
            )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16 / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            backdropUrl?.let { url ->
                Image(
                    painter = rememberAsyncImagePainter(model = url),
                    contentDescription = collection.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = collection.name,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(
                    text = "${collection.parts.size} movie" + if (collection.parts.size == 1) "" else "s",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
                collection.averageVote?.let { average ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Average rating",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "${(average * 10).toInt() / 10.0} / 10 average",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            if (collection.overview.isNotEmpty()) {
                Text(
                    text = collection.overview,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun CollectionMoviesList(
    collection: CollectionDetail,
    onMovieClicked: (Long) -> Unit,
) {
    if (collection.parts.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.section_movies),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        collection.partsInReleaseOrder.forEach { movie ->
            CollectionMovieItem(movie = movie, onClick = { onMovieClicked(movie.id.toLong()) })
        }
    }
}

@Composable
private fun CollectionMovieItem(
    movie: Movie,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        val density = LocalDensity.current.density
        val posterUrl =
            ImageConfigResolver.resolve(
                path = movie.posterPath,
                type = ImageConfigResolver.ImageType.POSTER,
                targetWidthDp = 90,
                density = density,
            )
        Box(
            modifier =
                Modifier
                    .width(80.dp)
                    .aspectRatio(2 / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val fallbackPainter = painterResource(Res.drawable.baseline_movie_24)
            val painter =
                rememberAsyncImagePainter(
                    model = posterUrl,
                    filterQuality = FilterQuality.Medium,
                    error = fallbackPainter,
                    fallback = fallbackPainter,
                )
            val painterState by painter.state.collectAsState()
            val contentScale =
                if (painterState is AsyncImagePainter.State.Success) {
                    ContentScale.Crop
                } else {
                    ContentScale.Fit
                }
            Image(
                painter = painter,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movie.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val year = movie.releaseDate.take(4)
            val rating = movie.voteAverage.takeIf { it > 0 }?.let { "${(it * 10).toInt() / 10.0} ★" }
            val subtitle = listOfNotNull(year.takeIf { it.isNotEmpty() }, rating).joinToString(" • ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = movie.overview.ifEmpty { stringResource(Res.string.no_overview_available) },
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
