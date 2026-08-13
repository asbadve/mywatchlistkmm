package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import com.ajinkyabadve.kmmmywatchlist.core.ui.collapsingTopBar
import com.ajinkyabadve.kmmmywatchlist.core.ui.rememberCollapsibleBarState
import com.ajinkyabadve.kmmmywatchlist.design.util.FullscreenMediaGallery
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.MovieImagesSection
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCredit
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.model.filmographySections
import com.ajinkyabadve.kmmmywatchlist.features.person.model.knownForCredits
import com.ajinkyabadve.kmmmywatchlist.openUrl
import com.ajinkyabadve.kmmmywatchlist.util.ImageDownloader
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_read_less
import mywatchlist.composeapp.generated.resources.action_read_more
import mywatchlist.composeapp.generated.resources.action_retry
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import mywatchlist.composeapp.generated.resources.filter_all
import mywatchlist.composeapp.generated.resources.filter_all_departments
import mywatchlist.composeapp.generated.resources.filter_movies
import mywatchlist.composeapp.generated.resources.filter_tv_shows
import mywatchlist.composeapp.generated.resources.no_filmography_matches
import mywatchlist.composeapp.generated.resources.section_biography
import mywatchlist.composeapp.generated.resources.section_filmography
import mywatchlist.composeapp.generated.resources.section_known_for
import mywatchlist.composeapp.generated.resources.section_photos
import mywatchlist.composeapp.generated.resources.title_person
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private object PersonDetailConstant {
    const val KNOWN_FOR_CAPTION_SEPARATOR = " · "
    const val MAX_CREDITS = 20

    // TMDB media_type identifiers (API values, not user-facing).
    const val MEDIA_TYPE_MOVIE = "movie"
    const val MEDIA_TYPE_TV = "tv"
    const val BIOGRAPHY_PREVIEW_LINES = 5
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Long,
    windowSize: WindowSize,
    onBackClicked: () -> Unit,
    onMovieClicked: (Long) -> Unit,
    onTvShowClicked: (Long) -> Unit,
    viewModel: PersonDetailScreenModel =
        viewModel(key = "PersonDetailScreenModel:$personId") { PersonDetailScreenModel(personId) },
) {
    val uiState by viewModel.uiState.collectAsState()
    var galleryImages by remember { mutableStateOf<List<String>?>(null) }
    var galleryInitialIndex by remember { mutableStateOf(0) }

    // Same hero treatment as the movie and TV detail screens: the bar floats transparently over the
    // banner and only goes solid once the list has scrolled past it. That means it cannot live in
    // Scaffold's topBar slot - that slot always reserves layout space, which would push the banner
    // down and stop it running under the status bar.
    val lazyListState = rememberLazyListState()
    val leftLazyListState = rememberLazyListState()
    val topBarState = rememberCollapsibleBarState()
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
                is PersonDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is PersonDetailState.Error -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { viewModel.loadPersonDetails() }) {
                            Text(stringResource(Res.string.action_retry))
                        }
                    }
                }

                is PersonDetailState.Success -> {
                    val onCreditClicked: (PersonCredit) -> Unit = { credit ->
                        if (credit.isMovie) onMovieClicked(credit.id) else onTvShowClicked(credit.id)
                    }
                    val onShowGallery: (List<String>, Int) -> Unit = { images, index ->
                        galleryImages = images
                        galleryInitialIndex = index
                    }
                    if (windowSize.isCompact()) {
                        CompactPersonDetailContent(
                            person = state.person,
                            lazyListState = lazyListState,
                            onCreditClicked = onCreditClicked,
                            onShowGallery = onShowGallery,
                        )
                    } else {
                        ExpandedPersonDetailContent(
                            person = state.person,
                            leftLazyListState = leftLazyListState,
                            onCreditClicked = onCreditClicked,
                            onShowGallery = onShowGallery,
                        )
                    }
                }
            }

            DetailTopBar(
                title = (uiState as? PersonDetailState.Success)?.person?.name ?: stringResource(Res.string.title_person),
                onBackClicked = onBackClicked,
                isScrolledPastHero = showSolidHeader,
                modifier = Modifier.collapsingTopBar(topBarState),
            )

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
private fun CompactPersonDetailContent(
    person: PersonDetail,
    lazyListState: LazyListState,
    onCreditClicked: (PersonCredit) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { PersonHeroSection(person = person, onCreditClicked = onCreditClicked) }
        item { PersonLinksRow(person = person) }
        item { BiographySection(biography = person.biography) }
        item { KnownForRow(person = person, onCreditClicked = onCreditClicked) }
        item {
            MovieImagesSection(
                images = person.images?.profiles ?: emptyList(),
                title = stringResource(Res.string.section_photos),
                imageType = ImageConfigResolver.ImageType.PROFILE,
                onShowGallery = onShowGallery,
            )
        }
        item { FilmographySection(person = person, onCreditClicked = onCreditClicked) }
    }
}

// Same half-and-half split the other detail screens use on non-compact widths: identity and
// biography on the left, the filmography on the right.
@Composable
private fun ExpandedPersonDetailContent(
    person: PersonDetail,
    leftLazyListState: LazyListState,
    onCreditClicked: (PersonCredit) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        LazyColumn(
            state = leftLazyListState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { PersonHeroSection(person = person, onCreditClicked = onCreditClicked) }
            item { PersonLinksRow(person = person) }
            item { BiographySection(biography = person.biography) }
            item {
                MovieImagesSection(
                    images = person.images?.profiles ?: emptyList(),
                    title = stringResource(Res.string.section_photos),
                    imageType = ImageConfigResolver.ImageType.PROFILE,
                    onShowGallery = onShowGallery,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp, top = 16.dp),
        ) {
            item { KnownForRow(person = person, onCreditClicked = onCreditClicked) }
            item { FilmographySection(person = person, onCreditClicked = onCreditClicked) }
        }
    }
}

/**
 * The person header, over a banner borrowed from the work they are best known for.
 *
 * The banner is a background layer sized to the header via matchParentSize, so the portrait, name
 * and facts keep exactly the position they had before and simply gain something to sit against.
 * People with no credit carrying a backdrop - newcomers, most crew - fall through to the plain
 * header rather than to an empty grey band.
 */
@Composable
private fun PersonLinksRow(person: PersonDetail) {
    val ids = person.externalIds
    val links =
        buildList {
            (person.imdbId ?: ids?.imdbId)?.let { add("IMDb" to "https://www.imdb.com/name/$it/") }
            person.homepage?.takeIf { it.isNotEmpty() }?.let { add("Homepage" to it) }
            ids?.instagramId?.let { add("Instagram" to "https://www.instagram.com/$it/") }
            ids?.twitterId?.let { add("X" to "https://x.com/$it") }
            ids?.facebookId?.let { add("Facebook" to "https://www.facebook.com/$it") }
            ids?.tiktokId?.let { add("TikTok" to "https://www.tiktok.com/@$it") }
            ids?.youtubeId?.let { add("YouTube" to "https://www.youtube.com/$it") }
        }
    if (links.isEmpty()) return
    // Deliberately lighter than Material's AssistChip. There can be seven of these, and at full
    // chip weight a row of outlined buttons reads as the screen's primary actions - which they are
    // not. Compact outline pills keep them reachable while making clear they are secondary.
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        links.forEach { (label, url) ->
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(16.dp),
                        ).clickable { openUrl(url) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun BiographySection(biography: String) {
    if (biography.isEmpty()) return
    // Long biographies collapse to a preview with a Read more toggle, like themoviedb.org.
    var expanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.section_biography),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = biography,
            fontSize = 14.sp,
            maxLines = if (expanded) Int.MAX_VALUE else PersonDetailConstant.BIOGRAPHY_PREVIEW_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult ->
                if (!expanded) hasOverflow = layoutResult.hasVisualOverflow
            },
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
        if (hasOverflow || expanded) {
            Text(
                text = stringResource(if (expanded) Res.string.action_read_less else Res.string.action_read_more),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .padding(top = 4.dp)
                        .clickable { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun KnownForRow(
    person: PersonDetail,
    onCreditClicked: (PersonCredit) -> Unit,
) {
    PersonCreditsRow(
        title = stringResource(Res.string.section_known_for),
        credits = person.knownForCredits(PersonDetailConstant.MAX_CREDITS),
        // Year alongside the role: the poster alone says which film, the caption says why this
        // credit is one they are known for.
        caption = { credit ->
            listOfNotNull(
                credit.displayDate?.take(4)?.takeIf { it.isNotEmpty() },
                credit.character?.takeIf { it.isNotEmpty() } ?: credit.job,
            ).joinToString(PersonDetailConstant.KNOWN_FOR_CAPTION_SEPARATOR).takeIf { it.isNotEmpty() }
        },
        onCreditClicked = onCreditClicked,
    )
}

// Full filmography like themoviedb.org person pages: "Acting" plus one subsection per crew
// department, every credit listed newest-first with its year, role and episode count.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilmographySection(
    person: PersonDetail,
    onCreditClicked: (PersonCredit) -> Unit,
) {
    if (person.combinedCredits
            ?.filmographySections()
            .orEmpty()
            .isEmpty()
    ) {
        return
    }

    // null = All for both filters, mirroring the media/department filters on themoviedb.org.
    var mediaFilter by remember { mutableStateOf<String?>(null) }
    var departmentFilter by remember { mutableStateOf<String?>(null) }

    val sections = person.combinedCredits?.filmographySections(mediaFilter).orEmpty()
    val visibleSections =
        if (departmentFilter == null) sections else sections.filter { it.first == departmentFilter }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
    ) {
        Text(
            text = stringResource(Res.string.section_filmography),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        FlowRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf<Pair<String, String?>>(
                stringResource(Res.string.filter_all) to null,
                stringResource(Res.string.filter_movies) to PersonDetailConstant.MEDIA_TYPE_MOVIE,
                stringResource(Res.string.filter_tv_shows) to PersonDetailConstant.MEDIA_TYPE_TV,
            ).forEach { (label, value) ->
                FilmographyFilterChip(
                    selected = mediaFilter == value,
                    label = label,
                    onClick = { mediaFilter = value },
                )
            }
        }
        // Department chips reflect what exists for the current media filter; a department that
        // disappears (e.g. no TV writing credits) resets the selection back to All.
        val departments =
            person.combinedCredits
                ?.filmographySections(mediaFilter)
                .orEmpty()
                .map { it.first }
        if (departmentFilter != null && departmentFilter !in departments) departmentFilter = null
        if (departments.size > 1) {
            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (listOf<String?>(null) + departments).forEach { department ->
                    FilmographyFilterChip(
                        selected = departmentFilter == department,
                        label = department ?: stringResource(Res.string.filter_all_departments),
                        onClick = { departmentFilter = department },
                    )
                }
            }
        }
        if (visibleSections.isEmpty()) {
            Text(
                text = stringResource(Res.string.no_filmography_matches),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        visibleSections.forEach { (department, credits) ->
            Text(
                text = department,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 4.dp),
            )
            credits.forEach { credit ->
                FilmographyRow(credit = credit, onClick = { onCreditClicked(credit) })
            }
        }
    }
}

@Composable
private fun FilmographyRow(
    credit: PersonCredit,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = credit.displayDate?.take(4)?.takeIf { it.isNotEmpty() } ?: "—",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.width(44.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = credit.displayTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val role =
                credit.character?.takeIf { it.isNotEmpty() }?.let { "as $it" }
                    ?: credit.job?.takeIf { it.isNotEmpty() }
            val episodes =
                credit.episodeCount?.takeIf { it > 0 }?.let { count ->
                    "$count episode" + if (count == 1) "" else "s"
                }
            val caption = listOfNotNull(credit.mediaTypeLabel, role, episodes).joinToString(" • ")
            if (caption.isNotEmpty()) {
                Text(
                    text = caption,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun PersonCreditsRow(
    title: String,
    credits: List<PersonCredit>,
    caption: (PersonCredit) -> String?,
    onCreditClicked: (PersonCredit) -> Unit,
) {
    if (credits.isEmpty()) return
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(credits) { credit ->
                PersonCreditCard(credit = credit, caption = caption(credit), onClick = { onCreditClicked(credit) })
            }
        }
    }
}

@Composable
private fun PersonCreditCard(
    credit: PersonCredit,
    caption: String?,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        val density = LocalDensity.current.density
        val posterUrl =
            ImageConfigResolver.resolve(
                path = credit.posterPath,
                type = ImageConfigResolver.ImageType.POSTER,
                targetWidthDp = 120,
                density = density,
            )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
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
                contentDescription = credit.displayTitle,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
            credit.mediaTypeLabel?.let { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = credit.displayTitle,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        val subtitle =
            listOfNotNull(
                credit.displayDate?.take(4)?.takeIf { it.isNotEmpty() },
                caption?.takeIf { it.isNotEmpty() },
            ).joinToString(" • ")
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
    }
}

// M3 filter chips show a leading checkmark while selected (m3.material.io/components/chips).
@Composable
private fun FilmographyFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon =
            if (selected) {
                {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = "Selected",
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
    )
}
