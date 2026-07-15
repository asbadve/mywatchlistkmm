package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.formatFullReleaseDate
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TvMetaSection(detail: TvDetail) {
    val contentRating = detail.contentRatings?.results
        ?.firstOrNull { it.iso3166 == "US" && it.rating.isNotEmpty() }
        ?.rating

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

        // Info details row (First air date, status, content rating, seasons/episodes)
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (detail.firstAirDate.isNotEmpty()) {
                val fullDate = formatFullReleaseDate(detail.firstAirDate)
                SuggestionChip(
                    onClick = {},
                    label = { Text(fullDate) }
                )
            }

            detail.status?.let {
                if (it.isNotEmpty()) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(it) }
                    )
                }
            }

            if (!contentRating.isNullOrEmpty()) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(contentRating) }
                )
            }

            detail.numberOfSeasons?.let { seasons ->
                if (seasons > 0) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(if (seasons == 1) "$seasons Season" else "$seasons Seasons") }
                    )
                }
            }

            detail.numberOfEpisodes?.let { episodes ->
                if (episodes > 0) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(if (episodes == 1) "$episodes Episode" else "$episodes Episodes") }
                    )
                }
            }
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

        // Networks Section
        detail.networks?.let { networkList ->
            if (networkList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Networks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = networkList.joinToString(", ") { it.name },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
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
