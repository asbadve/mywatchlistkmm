package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
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
import com.ajinkyabadve.kmmmywatchlist.design.util.FullscreenMediaGallery
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.MovieImagesSection
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCredit
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.model.filmographySections
import com.ajinkyabadve.kmmmywatchlist.features.person.model.knownForCredits
import com.ajinkyabadve.kmmmywatchlist.features.person.model.yearsBetween
import com.ajinkyabadve.kmmmywatchlist.openUrl
import com.ajinkyabadve.kmmmywatchlist.util.ImageDownloader
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_movie_24
import mywatchlist.composeapp.generated.resources.baseline_person_24
import org.jetbrains.compose.resources.painterResource

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

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        val title = (uiState as? PersonDetailState.Success)?.person?.name ?: "Person"
                        Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                HorizontalDivider()
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is PersonDetailState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is PersonDetailState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
                        Button(onClick = { viewModel.loadPersonDetails() }) {
                            Text("Retry")
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
                            onCreditClicked = onCreditClicked,
                            onShowGallery = onShowGallery,
                        )
                    } else {
                        ExpandedPersonDetailContent(
                            person = state.person,
                            onCreditClicked = onCreditClicked,
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
private fun CompactPersonDetailContent(
    person: PersonDetail,
    onCreditClicked: (PersonCredit) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { PersonHeader(person = person) }
        item { PersonLinksRow(person = person) }
        item { BiographySection(biography = person.biography) }
        item { KnownForRow(person = person, onCreditClicked = onCreditClicked) }
        item {
            MovieImagesSection(
                images = person.images?.profiles ?: emptyList(),
                title = "Photos",
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
    onCreditClicked: (PersonCredit) -> Unit,
    onShowGallery: (images: List<String>, index: Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { PersonHeader(person = person) }
            item { PersonLinksRow(person = person) }
            item { BiographySection(biography = person.biography) }
            item {
                MovieImagesSection(
                    images = person.images?.profiles ?: emptyList(),
                    title = "Photos",
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

@Composable
private fun PersonHeader(person: PersonDetail) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        val density = LocalDensity.current.density
        val profileUrl = ImageConfigResolver.resolve(
            path = person.profilePath,
            type = ImageConfigResolver.ImageType.PROFILE,
            targetWidthDp = 140,
            density = density,
        )
        Box(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(2 / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val fallbackPainter = painterResource(Res.drawable.baseline_person_24)
            val painter = rememberAsyncImagePainter(
                model = profileUrl,
                filterQuality = FilterQuality.Medium,
                error = fallbackPainter,
                fallback = fallbackPainter,
            )
            val painterState by painter.state.collectAsState()
            val contentScale = if (painterState is AsyncImagePainter.State.Success) {
                ContentScale.Crop
            } else {
                ContentScale.Fit
            }
            Image(
                painter = painter,
                contentDescription = person.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.name,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val subtitle = listOfNotNull(person.knownForDepartment, person.genderLabel)
                .filter { it.isNotEmpty() }
                .joinToString(" • ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            val deathday = person.deathday?.takeIf { it.isNotEmpty() }
            person.birthday?.takeIf { it.isNotEmpty() }?.let { birthday ->
                val place = person.placeOfBirth?.takeIf { it.isNotEmpty() }
                // Show current age while alive; the age at death is shown on the "Died" line.
                val age = if (deathday == null) {
                    yearsBetween(birthday, Clock.System.todayIn(TimeZone.currentSystemDefault()).toString())
                } else {
                    null
                }
                Text(
                    text = "Born $birthday" +
                        (age?.let { " (age $it)" } ?: "") +
                        (place?.let { " in $it" } ?: ""),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            deathday?.let {
                val ageAtDeath = yearsBetween(person.birthday, it)
                Text(
                    text = "Died $it" + (ageAtDeath?.let { age -> " (aged $age)" } ?: ""),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (person.knownCreditsCount > 0) {
                Text(
                    text = "${person.knownCreditsCount} known credits • Popularity ${(person.popularity * 10).toInt() / 10.0}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (person.alsoKnownAs.isNotEmpty()) {
                Text(
                    text = "Also known as ${person.alsoKnownAs.joinToString(", ")}",
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

// External links from the base payload (homepage, imdb_id) and the external_ids append. Only
// non-null handles become links.
@Composable
private fun PersonLinksRow(person: PersonDetail) {
    val ids = person.externalIds
    val links = buildList {
        (person.imdbId ?: ids?.imdbId)?.let { add("IMDb" to "https://www.imdb.com/name/$it/") }
        person.homepage?.takeIf { it.isNotEmpty() }?.let { add("Homepage" to it) }
        ids?.instagramId?.let { add("Instagram" to "https://www.instagram.com/$it/") }
        ids?.twitterId?.let { add("X" to "https://x.com/$it") }
        ids?.facebookId?.let { add("Facebook" to "https://www.facebook.com/$it") }
        ids?.tiktokId?.let { add("TikTok" to "https://www.tiktok.com/@$it") }
        ids?.youtubeId?.let { add("YouTube" to "https://www.youtube.com/$it") }
    }
    if (links.isEmpty()) return
    // Material 3 assist chips: the M3 chips guidance treats actions that hand off to another
    // app/site (opening IMDb, socials) as assist-chip territory, laid out as a wrapping group.
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        links.forEach { (label, url) ->
            AssistChip(
                onClick = { openUrl(url) },
                label = { Text(label) },
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = "Biography",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = biography,
            fontSize = 14.sp,
            maxLines = if (expanded) Int.MAX_VALUE else BIOGRAPHY_PREVIEW_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layoutResult ->
                if (!expanded) hasOverflow = layoutResult.hasVisualOverflow
            },
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
        if (hasOverflow || expanded) {
            Text(
                text = if (expanded) "Read less" else "Read more",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun KnownForRow(person: PersonDetail, onCreditClicked: (PersonCredit) -> Unit) {
    PersonCreditsRow(
        title = "Known For",
        credits = person.knownForCredits(MAX_CREDITS),
        caption = { it.character?.takeIf { c -> c.isNotEmpty() } ?: it.job },
        onCreditClicked = onCreditClicked,
    )
}

// Full filmography like themoviedb.org person pages: "Acting" plus one subsection per crew
// department, every credit listed newest-first with its year, role and episode count.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilmographySection(person: PersonDetail, onCreditClicked: (PersonCredit) -> Unit) {
    if (person.combinedCredits?.filmographySections().orEmpty().isEmpty()) return

    // null = All for both filters, mirroring the media/department filters on themoviedb.org.
    var mediaFilter by remember { mutableStateOf<String?>(null) }
    var departmentFilter by remember { mutableStateOf<String?>(null) }

    val sections = person.combinedCredits?.filmographySections(mediaFilter).orEmpty()
    val visibleSections =
        if (departmentFilter == null) sections else sections.filter { it.first == departmentFilter }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "Filmography",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf<Pair<String, String?>>("All" to null, "Movies" to "movie", "TV shows" to "tv")
                .forEach { (label, value) ->
                    FilmographyFilterChip(
                        selected = mediaFilter == value,
                        label = label,
                        onClick = { mediaFilter = value },
                    )
                }
        }
        // Department chips reflect what exists for the current media filter; a department that
        // disappears (e.g. no TV writing credits) resets the selection back to All.
        val departments = person.combinedCredits?.filmographySections(mediaFilter).orEmpty().map { it.first }
        if (departmentFilter != null && departmentFilter !in departments) departmentFilter = null
        if (departments.size > 1) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (listOf<String?>(null) + departments).forEach { department ->
                    FilmographyFilterChip(
                        selected = departmentFilter == department,
                        label = department ?: "All departments",
                        onClick = { departmentFilter = department },
                    )
                }
            }
        }
        if (visibleSections.isEmpty()) {
            Text(
                text = "No credits match the selected filters.",
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
private fun FilmographyRow(credit: PersonCredit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
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
            val role = credit.character?.takeIf { it.isNotEmpty() }?.let { "as $it" }
                ?: credit.job?.takeIf { it.isNotEmpty() }
            val episodes = credit.episodeCount?.takeIf { it > 0 }?.let { count ->
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
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
private fun PersonCreditCard(credit: PersonCredit, caption: String?, onClick: () -> Unit) {
    Column(modifier = Modifier.width(120.dp).clickable(onClick = onClick)) {
        val density = LocalDensity.current.density
        val posterUrl = ImageConfigResolver.resolve(
            path = credit.posterPath,
            type = ImageConfigResolver.ImageType.POSTER,
            targetWidthDp = 120,
            density = density,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2 / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val fallbackPainter = painterResource(Res.drawable.baseline_movie_24)
            val painter = rememberAsyncImagePainter(
                model = posterUrl,
                filterQuality = FilterQuality.Medium,
                error = fallbackPainter,
                fallback = fallbackPainter,
            )
            val painterState by painter.state.collectAsState()
            val contentScale = if (painterState is AsyncImagePainter.State.Success) {
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
                    modifier = Modifier
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
        val subtitle = listOfNotNull(
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
        leadingIcon = if (selected) {
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

private const val MAX_CREDITS = 20
private const val BIOGRAPHY_PREVIEW_LINES = 5
