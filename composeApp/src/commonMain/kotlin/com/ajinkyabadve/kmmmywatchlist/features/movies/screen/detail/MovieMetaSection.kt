package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvider
import com.ajinkyabadve.kmmmywatchlist.openUrl

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MovieMetaSection(detail: MovieDetail, onCollectionClicked: (Long) -> Unit = {}) {
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

        detail.tagline?.takeIf { it.isNotEmpty() }?.let { tagline ->
            Text(
                text = tagline,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        val crew = detail.credits?.crew.orEmpty()
        val directors = crew.filter { it.job == "Director" }.map { it.name }
        val writers = crew.filter { it.department == "Writing" }.map { it.name }.distinct()
        if (directors.isNotEmpty()) {
            Text(
                text = "Directed by ${directors.joinToString(", ")}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (writers.isNotEmpty()) {
            Text(
                text = "Written by ${writers.joinToString(", ")}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info details row (Release date, runtime, budget, certification, translations)
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(genreList) { genre ->
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
                text = "${(detail.voteAverage * 10).toInt() / 10.0} / 10" +
                    if (detail.voteCount > 0) " (${detail.voteCount} votes)" else "",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        MovieExternalLinks(detail = detail)
        MovieFactsSection(detail = detail)
        CollectionBanner(detail = detail, onCollectionClicked = onCollectionClicked)
        WhereToWatchSection(detail = detail)
    }
}

// External links as Material 3 assist chips, same pattern as the person screen.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieExternalLinks(detail: MovieDetail) {
    val ids = detail.externalIds
    val links = buildList {
        ids?.imdbId?.let { add("IMDb" to "https://www.imdb.com/title/$it/") }
        detail.homepage?.takeIf { it.isNotEmpty() }?.let { add("Homepage" to it) }
        ids?.instagramId?.let { add("Instagram" to "https://www.instagram.com/$it/") }
        ids?.twitterId?.let { add("X" to "https://x.com/$it") }
        ids?.facebookId?.let { add("Facebook" to "https://www.facebook.com/$it") }
    }
    if (links.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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

// The "Facts" panel from themoviedb.org movie pages: status, budget/revenue, companies, etc.
@Composable
private fun MovieFactsSection(detail: MovieDetail) {
    val facts = buildList {
        detail.status?.takeIf { it.isNotEmpty() }?.let { add("Status" to it) }
        detail.originalTitle
            ?.takeIf { it.isNotEmpty() && it != detail.title }
            ?.let { add("Original title" to it) }
        detail.spokenLanguages
            .mapNotNull { language -> language.englishName.takeIf { it.isNotEmpty() } }
            .takeIf { it.isNotEmpty() }
            ?.let { add("Spoken languages" to it.joinToString(", ")) }
        detail.budget?.takeIf { it > 0 }?.let { add("Budget" to formatMoney(it)) }
        detail.revenue?.takeIf { it > 0 }?.let { add("Revenue" to formatMoney(it)) }
        detail.productionCompanies
            .mapNotNull { company -> company.name.takeIf { it.isNotEmpty() } }
            .takeIf { it.isNotEmpty() }
            ?.let { add("Production companies" to it.joinToString(", ")) }
        detail.productionCountries
            .mapNotNull { country -> country.name.takeIf { it.isNotEmpty() } }
            .takeIf { it.isNotEmpty() }
            ?.let { add("Countries" to it.joinToString(", ")) }
    }
    if (facts.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = "Facts",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        facts.forEach { (label, value) ->
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.width(150.dp)
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CollectionBanner(detail: MovieDetail, onCollectionClicked: (Long) -> Unit) {
    val collection = detail.belongsToCollection ?: return
    val density = LocalDensity.current.density
    val backdropUrl = ImageConfigResolver.resolve(
        path = collection.backdropPath ?: collection.posterPath,
        type = ImageConfigResolver.ImageType.BACKDROP,
        targetWidthDp = 600,
        density = density
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onCollectionClicked(collection.id) }
    ) {
        backdropUrl?.let { url ->
            Image(
                painter = rememberAsyncImagePainter(model = url),
                contentDescription = collection.name,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text(
                text = "Part of the ${collection.name}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "View collection ›",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// "Where to watch" for the user's region (falling back to US, then any region with data).
// Tapping a provider opens TMDB's watch page, which carries the required JustWatch attribution.
@Composable
private fun WhereToWatchSection(detail: MovieDetail) {
    val regions = detail.watchProviders?.results.orEmpty()
    if (regions.isEmpty()) return
    val regionCode = Locale.current.region.uppercase()
    val region = regions[regionCode] ?: regions["US"] ?: regions.values.first()
    val groups = listOf(
        "Stream" to region.flatrate,
        "Free" to region.free,
        "With ads" to region.ads,
        "Rent" to region.rent,
        "Buy" to region.buy,
    ).filter { it.second.isNotEmpty() }
    if (groups.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = "Where to watch",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        groups.forEach { (label, providers) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.width(70.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(providers.sortedBy { it.displayPriority }) { provider ->
                        WatchProviderLogo(provider = provider, watchLink = region.link)
                    }
                }
            }
        }
        Text(
            text = "Streaming availability by JustWatch",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun WatchProviderLogo(provider: WatchProvider, watchLink: String) {
    val density = LocalDensity.current.density
    val logoUrl = ImageConfigResolver.resolve(
        path = provider.logoPath,
        type = ImageConfigResolver.ImageType.LOGO,
        targetWidthDp = 40,
        density = density
    ) ?: return
    Image(
        painter = rememberAsyncImagePainter(model = logoUrl),
        contentDescription = provider.providerName,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = watchLink.isNotEmpty()) { openUrl(watchLink) },
        contentScale = ContentScale.Crop
    )
}

// No java.text.NumberFormat in common code - group digits manually, TMDB amounts are USD.
private fun formatMoney(amount: Long): String {
    val grouped = amount.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$" + grouped
}
