package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.MoviesConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.mediaMovieRow
import com.ajinkyabadve.kmmmywatchlist.openUrl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import mywatchlist.composeapp.generated.resources.baseline_person_24
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movieId: Long,
    windowSize: WindowSize,
    onBackClicked: () -> Unit,
    onMovieClicked: (Long) -> Unit,
    viewModel: MovieDetailScreenModel = remember(movieId) { MovieDetailScreenModel(movieId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    val leftLazyListState = rememberLazyListState()

    val showSolidHeader by remember {
        derivedStateOf {
            val state = if (windowSize.isCompact()) lazyListState else leftLazyListState
            state.firstVisibleItemIndex > 0
        }
    }

    Scaffold(
        topBar = {}
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (windowSize.isCompact()) PaddingValues(0.dp) else innerPadding)
        ) {
            when (val state = uiState) {
                is MovieDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is MovieDetailState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
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
                        if (windowSize.isCompact()) {
                            CompactMovieDetailContent(
                                detail = detail,
                                lazyListState = lazyListState,
                                onMovieClicked = onMovieClicked
                            )
                        } else {
                            ExpandedMovieDetailContent(
                                detail = detail,
                                leftLazyListState = leftLazyListState,
                                onMovieClicked = onMovieClicked
                            )
                        }

                        val headerBgColor by animateColorAsState(
                            targetValue = if (showSolidHeader) MaterialTheme.colorScheme.background else Color.Transparent,
                            animationSpec = tween(durationMillis = 300)
                        )
                        TopAppBar(
                            title = {
                                AnimatedVisibility(
                                    visible = showSolidHeader,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Text(
                                        text = detail.title,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            navigationIcon = {
                                if (showSolidHeader) {
                                    IconButton(onClick = onBackClicked) {
                                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                } else {
                                    IconButton(
                                        onClick = onBackClicked,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color.White
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = headerBgColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMovieDetailContent(
    detail: MovieDetail,
    lazyListState: LazyListState,
    onMovieClicked: (Long) -> Unit
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            BackdropSection(detail = detail)
        }
        item {
            MovieMetaSection(detail = detail)
        }
        item {
            OverviewSection(detail = detail)
        }
        item {
            VideoClipsSection(videos = detail.videos?.results ?: emptyList())
        }
        item {
            MovieImagesSection(images = detail.images?.backdrops ?: emptyList())
        }
        item {
            CastSection(castList = detail.credits?.cast ?: emptyList())
        }
        item {
            RecommendationsSection(
                recommendations = detail.recommendations?.list ?: emptyList(),
                onMovieClicked = onMovieClicked
            )
        }
        item {
            SimilarMoviesSection(
                similarMovies = detail.similar?.list ?: emptyList(),
                onMovieClicked = onMovieClicked
            )
        }
        item {
            ReviewsSection(
                reviews = detail.reviews?.results ?: emptyList()
            )
        }
    }
}

@Composable
private fun ExpandedMovieDetailContent(
    detail: MovieDetail,
    leftLazyListState: LazyListState,
    onMovieClicked: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left Column (Main details pane) - Width takes up 60% of space
        LazyColumn(
            state = leftLazyListState,
            modifier = Modifier.weight(1.5f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                BackdropSection(detail = detail)
            }
            item {
                MovieMetaSection(detail = detail)
            }
            item {
                OverviewSection(detail = detail)
            }
            item {
                VideoClipsSection(videos = detail.videos?.results ?: emptyList())
            }
            item {
                MovieImagesSection(images = detail.images?.backdrops ?: emptyList())
            }
            item {
                ReviewsSection(
                    reviews = detail.reviews?.results ?: emptyList()
                )
            }
        }

        // Right Column (Supporting details pane) - Width takes up 40% of space
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 64.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CastSection(castList = detail.credits?.cast ?: emptyList())
            }
            item {
                RecommendationsSection(
                    recommendations = detail.recommendations?.list ?: emptyList(),
                    onMovieClicked = onMovieClicked
                )
            }
            item {
                SimilarMoviesSection(
                    similarMovies = detail.similar?.list ?: emptyList(),
                    onMovieClicked = onMovieClicked
                )
            }
        }
    }
}

@Composable
private fun BackdropSection(detail: MovieDetail) {
    val backdropUrl = detail.backdropPath?.let { "https://image.tmdb.org/t/p/w780/$it" }
    val fallbackPainter = painterResource(Res.drawable.baseline_movie_24)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val painter = rememberAsyncImagePainter(
            model = backdropUrl,
            filterQuality = FilterQuality.Medium,
            error = fallbackPainter,
            fallback = fallbackPainter
        )
        val painterState by painter.state.collectAsState()
        val contentScale = if (painterState is AsyncImagePainter.State.Success) {
            ContentScale.Crop
        } else {
            ContentScale.Fit
        }

        Image(
            painter = painter,
            contentDescription = "Backdrop for ${detail.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )

        // Gradient overlay for visual contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 100f
                    )
                )
        )

        // Trailer Play Button (overlaid in center of backdrop if youtube key is present)
        val youtubeTrailer = detail.videos?.results?.firstOrNull {
            it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true)
        }

        if (youtubeTrailer != null) {
            FloatingActionButton(
                onClick = {
                    val url = "https://www.youtube.com/watch?v=${youtubeTrailer.key}"
                    openUrl(url)
                },
                modifier = Modifier.align(Alignment.Center),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Trailer",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

private fun formatFullReleaseDate(rawDate: String): String {
    if (rawDate.length != 10) return rawDate
    val parts = rawDate.split("-")
    if (parts.size != 3) return rawDate
    val year = parts[0]
    val month = when (parts[1]) {
        "01" -> "Jan"
        "02" -> "Feb"
        "03" -> "Mar"
        "04" -> "Apr"
        "05" -> "May"
        "06" -> "Jun"
        "07" -> "Jul"
        "08" -> "Aug"
        "09" -> "Sep"
        "10" -> "Oct"
        "11" -> "Nov"
        "12" -> "Dec"
        else -> parts[1]
    }
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    return "$month $day, $year"
}

@Composable
private fun MovieMetaSection(detail: MovieDetail) {
    val certification = detail.releaseDates?.results
        ?.firstOrNull { it.iso3166 == "US" }
        ?.releaseDates
        ?.firstOrNull { it.certification.isNotEmpty() }
        ?.certification

    val translationsCount = detail.translations?.translations?.size ?: 0
    var showLanguagesDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Text(
            text = detail.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Info details row (Release date, runtime, budget, certification, translations)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (detail.releaseDate.isNotEmpty()) {
                val fullDate = formatFullReleaseDate(detail.releaseDate)
                SuggestionChip(
                    onClick = {},
                    label = { Text(fullDate) }
                )
            }

            if (!certification.isNullOrEmpty()) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(certification) }
                )
            }

            detail.runtime?.let {
                val formattedRuntime = formatRuntime(it)
                if (formattedRuntime.isNotEmpty()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(formattedRuntime) }
                    )
                }
            }

            if (translationsCount > 0) {
                SuggestionChip(
                    onClick = { showLanguagesDropdown = true },
                    label = { Text("$translationsCount Langs") }
                )
            }
        }

        if (showLanguagesDropdown) {
            AlertDialog(
                onDismissRequest = { showLanguagesDropdown = false },
                title = {
                    Text(
                        text = "Languages ($translationsCount)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Box(modifier = Modifier.heightIn(max = 280.dp)) {
                        val languages = detail.translations?.translations?.map {
                            it.englishName.ifEmpty { it.name }
                        }?.filter { it.isNotEmpty() }?.sorted() ?: emptyList()

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(languages) { lang ->
                                Text(
                                    text = lang,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguagesDropdown = false }) {
                        Text("Close")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Genres Section
        detail.genres?.let { genreList ->
            if (genreList.isNotEmpty()) {
                Text(
                    text = "Genres",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    genreList.take(4).forEach { genre ->
                        AssistChip(
                            onClick = {},
                            label = { Text(genre.name, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Keywords Section
        detail.keywords?.keywords?.let { keywordList ->
            if (keywordList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keywords",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(keywordList.take(10)) { keyword ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(keyword.name, fontSize = 11.sp) },
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Rating Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Rating",
                tint = Color(0xFFFFD700)
            )
            Text(
                text = "${(detail.voteAverage * 10).toInt() / 10.0} / 10",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun OverviewSection(detail: MovieDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = "Overview",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = detail.overview.ifEmpty { "No overview available for this movie." },
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun CastSection(castList: List<CastMember>) {
    if (castList.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "Cast & Crew",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(castList.take(15)) { member ->
                    CastMemberItem(member = member)
                }
            }
        }
    }
}

@Composable
private fun CastMemberItem(member: CastMember) {
    val profileUrl = member.profilePath?.let { "https://image.tmdb.org/t/p/w185/$it" }
    val fallbackPainter = painterResource(Res.drawable.baseline_person_24)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        val painter = rememberAsyncImagePainter(
            model = profileUrl,
            filterQuality = FilterQuality.Medium,
            error = fallbackPainter,
            fallback = fallbackPainter
        )
        val painterState by painter.state.collectAsState()
        val contentScale = if (painterState is AsyncImagePainter.State.Success) {
            ContentScale.Crop
        } else {
            ContentScale.Fit
        }

        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Image(
                painter = painter,
                contentDescription = member.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = member.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = member.character,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecommendationsSection(
    recommendations: List<com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie>,
    onMovieClicked: (Long) -> Unit
) {
    if (recommendations.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "Recommendations",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(recommendations.take(10)) { movie ->
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

private fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return ""
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) {
        "${hours}h ${remainingMinutes}m"
    } else {
        "${minutes}m"
    }
}

private fun formatBudget(budget: Long?): String {
    if (budget == null || budget <= 0) return ""
    return if (budget >= 1_000_000) {
        "$${budget / 1_000_000}M"
    } else {
        "$$budget"
    }
}

@Composable
private fun SimilarMoviesSection(
    similarMovies: List<com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie>,
    onMovieClicked: (Long) -> Unit
) {
    if (similarMovies.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "Similar Movies",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
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

@Composable
private fun ReviewsSection(reviews: List<com.ajinkyabadve.kmmmywatchlist.features.movies.model.Review>) {
    if (reviews.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "Reviews",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                reviews.take(3).forEach { review ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "A review by ${review.author}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = review.content,
                                fontSize = 13.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoClipsSection(videos: List<com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult>) {
    val clips = videos.filter { it.site.lowercase() == "youtube" }
    if (clips.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "Trailers & Clips",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(clips.take(5)) { video ->
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .aspectRatio(16 / 9f)
                            .clickable {
                                openUrl("https://www.youtube.com/watch?v=${video.key}")
                            },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val thumbUrl = "https://img.youtube.com/vi/${video.key}/hqdefault.jpg"
                            val painter = rememberAsyncImagePainter(model = thumbUrl)
                            Image(
                                painter = painter,
                                contentDescription = video.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(36.dp),
                                tint = Color.White
                            )
                            Text(
                                text = video.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieImagesSection(images: List<com.ajinkyabadve.kmmmywatchlist.features.movies.model.BackdropImage>) {
    if (images.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "Photos",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
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


