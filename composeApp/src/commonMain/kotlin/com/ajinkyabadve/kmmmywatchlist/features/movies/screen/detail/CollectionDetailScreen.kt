package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.asString
import com.ajinkyabadve.kmmmywatchlist.core.notification.NotificationScheduler
import com.ajinkyabadve.kmmmywatchlist.core.notification.rememberNotificationPermissionRequester
import com.ajinkyabadve.kmmmywatchlist.core.ui.DetailTopBar
import com.ajinkyabadve.kmmmywatchlist.core.ui.MediaListRow
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.NotificationOptInDialog
import com.ajinkyabadve.kmmmywatchlist.design.util.FullscreenMediaGallery
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CollectionDetail
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.NotificationSettingsRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.NotificationSettingsRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.util.ImageDownloader
import kotlinx.coroutines.launch
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.collection_alert_prompt_body
import mywatchlist.composeapp.generated.resources.collection_alert_prompt_title
import mywatchlist.composeapp.generated.resources.featured_cast
import mywatchlist.composeapp.generated.resources.featured_crew
import mywatchlist.composeapp.generated.resources.section_images
import mywatchlist.composeapp.generated.resources.section_movies
import mywatchlist.composeapp.generated.resources.title_collection
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
    notificationSettingsRepository: NotificationSettingsRepository = NotificationSettingsRepositoryImpl(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var galleryImages by remember { mutableStateOf<List<String>?>(null) }
    var galleryInitialIndex by remember { mutableStateOf(0) }

    // Hoisted here, not inside the opt-in dialog's own `if` block below - same reasoning as
    // PersonDetailScreen's identical hoist: onConfirm calls a permission request that must survive
    // the dialog's own recomposition tearing that `if` block down.
    val notificationPermissionRequester = rememberNotificationPermissionRequester()
    val notificationCoroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    // Hide-on-scroll-down / reveal-on-scroll-up, matching the app's collapsing bottom nav.
    //
    // `canScroll` is not optional: left at its default of `{ true }`, Material3 moves the bar for
    // any drag, so a short collection whose content fits would lose its bar - and its only back
    // affordance - without ever having scrolled. See CollapsibleBarState for the same fix on the
    // bars this app drives itself.
    val scrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(
            canScroll = { listState.canScrollForward || listState.canScrollBackward },
        )

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
                    val isFollowing by viewModel.isFollowingCollection.collectAsState(initial = false)
                    var showNotificationOptInPrompt by remember { mutableStateOf(false) }
                    val onFollowClick = {
                        val justFollowed = viewModel.toggleFollowCollection(state.collection, isFollowing)
                        // Only offer the prompt on the click that actually turns following ON, and
                        // only if there's something to ask about - same reasoning as
                        // PersonDetailScreen's identical gate.
                        if (justFollowed &&
                            !notificationSettingsRepository.isEpisodeNotificationsEnabled() &&
                            !notificationSettingsRepository.hasSeenEpisodeAlertOptInPrompt()
                        ) {
                            showNotificationOptInPrompt = true
                        }
                    }
                    if (windowSize.isCompact()) {
                        CompactCollectionDetailContent(
                            listState = listState,
                            state = state,
                            onMovieClicked = onMovieClicked,
                            onPersonClicked = onPersonClicked,
                            onShowGallery = onShowGallery,
                            isFollowing = isFollowing,
                            onFollowClick = onFollowClick,
                        )
                    } else {
                        ExpandedCollectionDetailContent(
                            state = state,
                            onMovieClicked = onMovieClicked,
                            onPersonClicked = onPersonClicked,
                            onShowGallery = onShowGallery,
                            isFollowing = isFollowing,
                            onFollowClick = onFollowClick,
                        )
                    }

                    if (showNotificationOptInPrompt) {
                        NotificationOptInDialog(
                            title = stringResource(Res.string.collection_alert_prompt_title, state.collection.name),
                            body = stringResource(Res.string.collection_alert_prompt_body, state.collection.name),
                            onConfirm = {
                                showNotificationOptInPrompt = false
                                notificationCoroutineScope.launch {
                                    // Only marked "seen" once the OS permission is actually granted -
                                    // same reasoning as PersonDetailScreen's identical confirm flow.
                                    if (notificationPermissionRequester.request()) {
                                        notificationSettingsRepository.setEpisodeNotificationsEnabled(true)
                                        notificationSettingsRepository.markEpisodeAlertOptInPromptSeen()
                                        NotificationScheduler.schedule()
                                    }
                                }
                            },
                            onDismiss = {
                                notificationSettingsRepository.markEpisodeAlertOptInPromptSeen()
                                showNotificationOptInPrompt = false
                            },
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
    listState: LazyListState,
    onMovieClicked: (Long) -> Unit,
    onPersonClicked: (Long) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
) {
    val collection = state.collection
    LazyColumn(
        // Hoisted so the top bar's `canScroll` can ask whether this list has anywhere to go.
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { CollectionHeader(collection = collection, isFollowing = isFollowing, onFollowClick = onFollowClick) }
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
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
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
            item { CollectionHeader(collection = collection, isFollowing = isFollowing, onFollowClick = onFollowClick) }
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
private fun CollectionHeader(
    collection: CollectionDetail,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
) {
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

            FollowCollectionButton(
                isFollowing = isFollowing,
                onToggleClick = onFollowClick,
                modifier = Modifier.padding(top = 8.dp),
            )

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
            val year = movie.releaseDate.take(4)
            val rating = movie.voteAverage.takeIf { it > 0 }?.let { "${(it * 10).toInt() / 10.0} ★" }
            val yearAndRating = listOfNotNull(year.takeIf { it.isNotEmpty() }, rating).joinToString(" • ")
            MediaListRow(
                title = movie.title,
                posterPath = movie.posterPath,
                yearAndRating = yearAndRating,
                overview = movie.overview,
                onClick = { onMovieClicked(movie.id.toLong()) },
            )
        }
    }
}
