package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.CastSection
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.MovieImagesSection
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.OverviewSection
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.VideoClipsSection
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvSeasonDetail
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.section_backdrops
import mywatchlist.composeapp.generated.resources.section_posters
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvDetailScreen(
    tvShowId: Long,
    windowSize: WindowSize,
    onBackClicked: () -> Unit,
    onTvShowClicked: (Long) -> Unit,
    onViewAllSeasonsClick: (Long) -> Unit,
    onPersonClicked: (Long) -> Unit = {},
    viewModel: TvDetailScreenModel = viewModel(key = "TvDetailScreenModel:$tvShowId") { TvDetailScreenModel(tvShowId) },
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
                is TvDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is TvDetailState.Error -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { viewModel.loadTvDetails() }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }

                is TvDetailState.Success -> {
                    val detail = state.tvDetail
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
                                CompactTvDetailContent(
                                    detail = detail,
                                    currentSeason = state.currentSeason,
                                    lazyListState = lazyListState,
                                    onTvShowClicked = onTvShowClicked,
                                    onPersonClicked = onPersonClicked,
                                    onShowGallery = { images, index ->
                                        galleryImages = images
                                        galleryInitialIndex = index
                                    },
                                    onViewAllSeasonsClick = { onViewAllSeasonsClick(tvShowId) },
                                )
                            } else {
                                ExpandedTvDetailContent(
                                    detail = detail,
                                    currentSeason = state.currentSeason,
                                    leftLazyListState = leftLazyListState,
                                    onTvShowClicked = onTvShowClicked,
                                    onPersonClicked = onPersonClicked,
                                    onShowGallery = { images, index ->
                                        galleryImages = images
                                        galleryInitialIndex = index
                                    },
                                    onViewAllSeasonsClick = { onViewAllSeasonsClick(tvShowId) },
                                    rightColumnTopPadding = topBarHeightDp,
                                )
                            }

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned {
                                            topBarHeightDp = with(density) { it.size.height.toDp() }
                                        },
                            ) {
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
                                if (showSolidHeader) {
                                    HorizontalDivider()
                                }
                            }
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
private fun CompactTvDetailContent(
    detail: TvDetail,
    currentSeason: TvSeasonDetail?,
    lazyListState: LazyListState,
    onTvShowClicked: (Long) -> Unit,
    onPersonClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
    onViewAllSeasonsClick: () -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            TvBackdropSection(detail = detail)
        }
        item {
            TvMetaSection(detail = detail)
        }
        item {
            OverviewSection(overview = detail.overview)
        }
        item {
            VideoClipsSection(videos = detail.videos?.results ?: emptyList())
        }
        currentSeason?.let { season ->
            item {
                CurrentSeasonSection(
                    season = season,
                    onViewAllSeasonsClick = onViewAllSeasonsClick,
                )
            }
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
            TvRecommendationsSection(
                recommendations = detail.recommendations?.list ?: emptyList(),
                onTvShowClicked = onTvShowClicked,
            )
        }
        item {
            TvSimilarSection(
                similarTvShows = detail.similar?.list ?: emptyList(),
                onTvShowClicked = onTvShowClicked,
            )
        }
    }
}

@Composable
private fun ExpandedTvDetailContent(
    detail: TvDetail,
    currentSeason: TvSeasonDetail?,
    leftLazyListState: LazyListState,
    onTvShowClicked: (Long) -> Unit,
    onPersonClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
    onViewAllSeasonsClick: () -> Unit,
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
                TvBackdropSection(detail = detail)
            }
            item {
                TvMetaSection(detail = detail)
            }
            item {
                OverviewSection(overview = detail.overview)
            }
            item {
                VideoClipsSection(videos = detail.videos?.results ?: emptyList())
            }
            currentSeason?.let { season ->
                item {
                    CurrentSeasonSection(
                        season = season,
                        onViewAllSeasonsClick = onViewAllSeasonsClick,
                    )
                }
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
                TvRecommendationsSection(
                    recommendations = detail.recommendations?.list ?: emptyList(),
                    onTvShowClicked = onTvShowClicked,
                )
            }
            item {
                TvSimilarSection(
                    similarTvShows = detail.similar?.list ?: emptyList(),
                    onTvShowClicked = onTvShowClicked,
                )
            }
        }
    }
}
