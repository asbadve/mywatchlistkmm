package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MovieMetaSection(detail: MovieDetail) {
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
