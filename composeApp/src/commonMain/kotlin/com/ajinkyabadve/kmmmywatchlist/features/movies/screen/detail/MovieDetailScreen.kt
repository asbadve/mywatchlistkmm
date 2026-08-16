package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.core.ui.DetailTopBar
import com.ajinkyabadve.kmmmywatchlist.core.ui.collapsingTopBar
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.MediaActionButtonsSection
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.MediaActionsState
import com.ajinkyabadve.kmmmywatchlist.core.ui.rememberCollapsibleBarState
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepository
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.section_backdrops
import mywatchlist.composeapp.generated.resources.section_posters
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Long,
    windowSize: WindowSize,
    onBackClicked: () -> Unit,
    onMovieClicked: (Long) -> Unit,
    authRepository: AuthRepository = AuthRepositoryImpl(),
    onPersonClicked: (Long) -> Unit = {},
    onCollectionClicked: (Long) -> Unit = {},
    viewModel: MovieDetailScreenModel =
        viewModel(key = "MovieDetailScreenModel:$movieId") { MovieDetailScreenModel(movieId, authRepository = authRepository) },
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    val leftLazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // This top bar is an overlay inside the content (not Scaffold's topBar), because its
    // measured height feeds the split layout's right column - so Material3's
    // enterAlwaysScrollBehavior cannot drive it and CollapsibleBarState does instead.
    val topBarState = rememberCollapsibleBarState()

    var galleryImages by remember { mutableStateOf<List<String>?>(null) }
    var galleryInitialIndex by remember { mutableStateOf(0) }

    val showSolidHeader by remember {
        derivedStateOf {
            val state = if (windowSize.isCompact()) lazyListState else leftLazyListState
            state.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(topBarState.nestedScrollConnection),
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
                        Text(state.message.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { viewModel.loadMovieDetails() }) {
                            Text(stringResource(Res.string.action_retry))
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
                        var topBarHeightDp by remember { mutableStateOf(64.dp) }

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
                                    regionCode = state.regionCode,
                                    fallbackRegionCode = state.fallbackRegionCode,
                                    lazyListState = lazyListState,
                                    authRepository = authRepository,
                                    mediaActionsState = viewModel.mediaActionsState,
                                    onMovieClicked = onMovieClicked,
                                    onPersonClicked = onPersonClicked,
                                    onCollectionClicked = onCollectionClicked,
                                    onShowGallery = { images, index ->
                                        galleryImages = images
                                        galleryInitialIndex = index
                                    },
                                )
                            } else {
                                ExpandedMovieDetailContent(
                                    detail = detail,
                                    regionCode = state.regionCode,
                                    fallbackRegionCode = state.fallbackRegionCode,
                                    leftLazyListState = leftLazyListState,
                                    authRepository = authRepository,
                                    mediaActionsState = viewModel.mediaActionsState,
                                    onMovieClicked = onMovieClicked,
                                    onPersonClicked = onPersonClicked,
                                    onCollectionClicked = onCollectionClicked,
                                    onShowGallery = { images, index ->
                                        galleryImages = images
                                        galleryInitialIndex = index
                                    },
                                    rightColumnTopPadding = topBarHeightDp,
                                )
                            }

                            DetailTopBar(
                                title = detail.title,
                                onBackClicked = onBackClicked,
                                isScrolledPastHero = showSolidHeader,
                                modifier =
                                    Modifier
                                        .collapsingTopBar(topBarState)
                                        .onGloballyPositioned {
                                            topBarHeightDp = with(density) { it.size.height.toDp() }
                                        },
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
    regionCode: String,
    fallbackRegionCode: String,
    lazyListState: LazyListState,
    authRepository: AuthRepository,
    mediaActionsState: MediaActionsState,
    onMovieClicked: (Long) -> Unit,
    onPersonClicked: (Long) -> Unit,
    onCollectionClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            MovieHeroSection(detail = detail, regionCode = regionCode, fallbackRegionCode = fallbackRegionCode) { colors ->
                MediaActionButtonsSection(
                    mediaId = detail.id.toLong(),
                    colors = colors,
                    showAddToList = true,
                    authRepository = authRepository,
                    mediaActionsState = mediaActionsState,
                )
            }
        }
        item {
            MovieMetaSection(
                detail = detail,
                regionCode = regionCode,
                fallbackRegionCode = fallbackRegionCode,
                onCollectionClicked = onCollectionClicked,
            )
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
                title = stringResource(Res.string.section_backdrops),
                imageType = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.BACKDROP,
                onShowGallery = onShowGallery,
            )
        }
        item {
            MovieImagesSection(
                images = detail.images?.posters ?: emptyList(),
                title = stringResource(Res.string.section_posters),
                imageType = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.POSTER,
                onShowGallery = onShowGallery,
            )
        }
        item {
            CastSection(castList = detail.credits?.cast ?: emptyList(), onPersonClicked = onPersonClicked)
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
    regionCode: String,
    fallbackRegionCode: String,
    leftLazyListState: LazyListState,
    authRepository: AuthRepository,
    mediaActionsState: MediaActionsState,
    onMovieClicked: (Long) -> Unit,
    onPersonClicked: (Long) -> Unit,
    onCollectionClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
    rightColumnTopPadding: androidx.compose.ui.unit.Dp,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Left Column (Main details pane) - Width takes up half of space
        LazyColumn(
            state = leftLazyListState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                MovieHeroSection(detail = detail, regionCode = regionCode, fallbackRegionCode = fallbackRegionCode) { colors ->
                    MediaActionButtonsSection(
                        mediaId = detail.id.toLong(),
                        colors = colors,
                        showAddToList = true,
                        authRepository = authRepository,
                        mediaActionsState = mediaActionsState,
                    )
                }
            }
            item {
                MovieMetaSection(
                    detail = detail,
                    regionCode = regionCode,
                    fallbackRegionCode = fallbackRegionCode,
                    onCollectionClicked = onCollectionClicked,
                )
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
                    title = stringResource(Res.string.section_backdrops),
                    imageType = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.BACKDROP,
                    onShowGallery = onShowGallery,
                )
            }
            item {
                MovieImagesSection(
                    images = detail.images?.posters ?: emptyList(),
                    title = stringResource(Res.string.section_posters),
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

        // Right Column (Supporting details pane) - Width takes up half of space
        LazyColumn(
            modifier = Modifier.weight(1f).padding(top = rightColumnTopPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CastSection(castList = detail.credits?.cast ?: emptyList(), onPersonClicked = onPersonClicked)
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
