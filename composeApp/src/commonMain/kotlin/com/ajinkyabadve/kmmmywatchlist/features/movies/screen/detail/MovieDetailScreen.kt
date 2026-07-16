package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Long,
    windowSize: WindowSize,
    onBackClicked: () -> Unit,
    onMovieClicked: (Long) -> Unit,
    viewModel: MovieDetailScreenModel = viewModel(key = movieId.toString()) { MovieDetailScreenModel(movieId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    val leftLazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var galleryImages by remember { mutableStateOf<List<String>?>(null) }
    var galleryInitialIndex by remember { mutableStateOf(0) }

    val showSolidHeader by remember {
        derivedStateOf {
            val state = if (windowSize.isCompact()) lazyListState else leftLazyListState
            state.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        topBar = {},
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(if (windowSize.isCompact()) PaddingValues(0.dp) else innerPadding),
        ) {
            when (val state = uiState) {
                is MovieDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is MovieDetailState.Error -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { viewModel.loadMovieDetails() }) {
                            Text("Retry")
                        }
                    }
                }

                is MovieDetailState.Success -> {
                    val detail = state.movieDetail
                    Box(modifier = Modifier.fillMaxSize()) {
                        val headerBgColor by animateColorAsState(
                            targetValue = if (showSolidHeader) MaterialTheme.colorScheme.background else Color.Transparent,
                            animationSpec = tween(durationMillis = 300),
                        )

                        var verticalDragOffset by remember { mutableStateOf(0f) }
                        val density = androidx.compose.ui.platform.LocalDensity.current

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .offset {
                                        androidx.compose.ui.unit
                                            .IntOffset(0, verticalDragOffset.toInt())
                                    }.pointerInput(windowSize.isCompact(), lazyListState) {
                                        if (windowSize.isCompact()) {
                                            detectVerticalDragGestures(
                                                onDragStart = {},
                                                onDragEnd = {
                                                    val thresholdPx = with(density) { 150.dp.toPx() }
                                                    if (verticalDragOffset > thresholdPx) {
                                                        onBackClicked()
                                                    } else {
                                                        verticalDragOffset = 0f
                                                    }
                                                },
                                                onDragCancel = {
                                                    verticalDragOffset = 0f
                                                },
                                                onVerticalDrag = { change, dragAmount ->
                                                    val isAtTop =
                                                        lazyListState.firstVisibleItemIndex == 0 &&
                                                            lazyListState.firstVisibleItemScrollOffset == 0
                                                    if (isAtTop && (dragAmount > 0 || verticalDragOffset > 0)) {
                                                        verticalDragOffset = (verticalDragOffset + dragAmount).coerceAtLeast(0f)
                                                        change.consume()
                                                    }
                                                },
                                            )
                                        }
                                    },
                        ) {
                            if (windowSize.isCompact()) {
                                CompactMovieDetailContent(
                                    detail = detail,
                                    lazyListState = lazyListState,
                                    onMovieClicked = onMovieClicked,
                                    onShowGallery = { images, index ->
                                        galleryImages = images
                                        galleryInitialIndex = index
                                    },
                                )
                            } else {
                                ExpandedMovieDetailContent(
                                    detail = detail,
                                    leftLazyListState = leftLazyListState,
                                    onMovieClicked = onMovieClicked,
                                    onShowGallery = { images, index ->
                                        galleryImages = images
                                        galleryInitialIndex = index
                                    },
                                )
                            }

                            TopAppBar(
                                title = {
                                    AnimatedVisibility(
                                        visible = showSolidHeader,
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                    ) {
                                        Text(
                                            text = detail.title,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                },
                                navigationIcon = {
                                    if (showSolidHeader) {
                                        IconButton(onClick = onBackClicked) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                        }
                                    } else {
                                        IconButton(
                                            onClick = onBackClicked,
                                            modifier =
                                                Modifier
                                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = Color.White,
                                            )
                                        }
                                    }
                                },
                                colors =
                                    TopAppBarDefaults.topAppBarColors(
                                        containerColor = headerBgColor,
                                    ),
                            )
                        }
                    }
                }
            }

            // Fullscreen Media Gallery Overlay
            galleryImages?.let { images ->
                com.ajinkyabadve.kmmmywatchlist.design.util.FullscreenMediaGallery(
                    images = images,
                    initialIndex = galleryInitialIndex,
                    onDismiss = { galleryImages = null },
                    onDownload = { imageUrl ->
                        com.ajinkyabadve.kmmmywatchlist.util.ImageDownloader
                            .downloadAndSave(imageUrl)
                    },
                )
            }
        }
    }
}

@Composable
private fun CompactMovieDetailContent(
    detail: MovieDetail,
    lazyListState: LazyListState,
    onMovieClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            BackdropSection(detail = detail)
        }
        item {
            MovieMetaSection(detail = detail)
        }
        item {
            OverviewSection(overview = detail.overview)
        }
        item {
            VideoClipsSection(videos = detail.videos?.results ?: emptyList())
        }
        item {
            MovieImagesSection(
                images = detail.images?.backdrops ?: emptyList(),
                title = "Backdrops",
                imageType = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.BACKDROP,
                onShowGallery = onShowGallery,
            )
        }
        item {
            MovieImagesSection(
                images = detail.images?.posters ?: emptyList(),
                title = "Posters",
                imageType = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.POSTER,
                onShowGallery = onShowGallery,
            )
        }
        item {
            CastSection(castList = detail.credits?.cast ?: emptyList())
        }
        item {
            RecommendationsSection(
                recommendations = detail.recommendations?.list ?: emptyList(),
                onMovieClicked = onMovieClicked,
            )
        }
        item {
            SimilarMoviesSection(
                similarMovies = detail.similar?.list ?: emptyList(),
                onMovieClicked = onMovieClicked,
            )
        }
        item {
            ReviewsSection(
                reviews = detail.reviews?.results ?: emptyList(),
            )
        }
    }
}

@Composable
private fun ExpandedMovieDetailContent(
    detail: MovieDetail,
    leftLazyListState: LazyListState,
    onMovieClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Left Column (Main details pane) - Width takes up 60% of space
        LazyColumn(
            state = leftLazyListState,
            modifier = Modifier.weight(1.5f),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                BackdropSection(detail = detail)
            }
            item {
                MovieMetaSection(detail = detail)
            }
            item {
                OverviewSection(overview = detail.overview)
            }
            item {
                VideoClipsSection(videos = detail.videos?.results ?: emptyList())
            }
            item {
                MovieImagesSection(
                    images = detail.images?.backdrops ?: emptyList(),
                    title = "Backdrops",
                    imageType = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.BACKDROP,
                    onShowGallery = onShowGallery,
                )
            }
            item {
                MovieImagesSection(
                    images = detail.images?.posters ?: emptyList(),
                    title = "Posters",
                    imageType = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.POSTER,
                    onShowGallery = onShowGallery,
                )
            }
            item {
                ReviewsSection(
                    reviews = detail.reviews?.results ?: emptyList(),
                )
            }
        }

        // Right Column (Supporting details pane) - Width takes up 40% of space
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 64.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CastSection(castList = detail.credits?.cast ?: emptyList())
            }
            item {
                RecommendationsSection(
                    recommendations = detail.recommendations?.list ?: emptyList(),
                    onMovieClicked = onMovieClicked,
                )
            }
            item {
                SimilarMoviesSection(
                    similarMovies = detail.similar?.list ?: emptyList(),
                    onMovieClicked = onMovieClicked,
                )
            }
        }
    }
}
