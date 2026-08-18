package com.ajinkyabadve.kmmmywatchlist.features.discover.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Keyword
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.KeywordRepository
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.KeywordRepositoryImpl
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.discover_filter_apply
import mywatchlist.composeapp.generated.resources.discover_filter_clear
import mywatchlist.composeapp.generated.resources.discover_filter_genres_label
import mywatchlist.composeapp.generated.resources.discover_filter_keywords_hint
import mywatchlist.composeapp.generated.resources.discover_filter_keywords_label
import mywatchlist.composeapp.generated.resources.discover_filter_sort_by_label
import mywatchlist.composeapp.generated.resources.discover_filter_year_any
import mywatchlist.composeapp.generated.resources.discover_filter_year_label
import mywatchlist.composeapp.generated.resources.discover_sort_popularity_asc
import mywatchlist.composeapp.generated.resources.discover_sort_popularity_desc
import mywatchlist.composeapp.generated.resources.discover_sort_release_date_asc
import mywatchlist.composeapp.generated.resources.discover_sort_release_date_desc
import mywatchlist.composeapp.generated.resources.discover_sort_revenue_asc
import mywatchlist.composeapp.generated.resources.discover_sort_revenue_desc
import mywatchlist.composeapp.generated.resources.discover_sort_title_asc
import mywatchlist.composeapp.generated.resources.discover_sort_title_desc
import mywatchlist.composeapp.generated.resources.discover_sort_vote_average_asc
import mywatchlist.composeapp.generated.resources.discover_sort_vote_average_desc
import mywatchlist.composeapp.generated.resources.discover_sort_vote_count_asc
import mywatchlist.composeapp.generated.resources.discover_sort_vote_count_desc
import mywatchlist.composeapp.generated.resources.discover_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private object DiscoverFilterDialogConstant {
    const val YEAR_RANGE_SIZE = 60
    const val KEYWORD_DEBOUNCE_MILLIS = 350L
    val DIALOG_MAX_HEIGHT = 520.dp
    val SUGGESTIONS_MAX_HEIGHT = 160.dp
}

/** Maps a TMDB `sort_by` value to its display label. Falls back to the raw value if unmapped. */
fun sortByLabelResource(sortBy: String): StringResource? =
    when (sortBy) {
        "popularity.desc" -> Res.string.discover_sort_popularity_desc
        "popularity.asc" -> Res.string.discover_sort_popularity_asc
        "vote_average.desc" -> Res.string.discover_sort_vote_average_desc
        "vote_average.asc" -> Res.string.discover_sort_vote_average_asc
        "vote_count.desc" -> Res.string.discover_sort_vote_count_desc
        "vote_count.asc" -> Res.string.discover_sort_vote_count_asc
        "primary_release_date.desc", "first_air_date.desc" -> Res.string.discover_sort_release_date_desc
        "primary_release_date.asc", "first_air_date.asc" -> Res.string.discover_sort_release_date_asc
        "revenue.desc" -> Res.string.discover_sort_revenue_desc
        "revenue.asc" -> Res.string.discover_sort_revenue_asc
        "title.asc", "name.asc" -> Res.string.discover_sort_title_asc
        "title.desc", "name.desc" -> Res.string.discover_sort_title_desc
        else -> null
    }

/**
 * Pending-state filter popup: nothing here is written back to the owning ScreenModel until
 * "Apply" is tapped, so picking several genres/a keyword/etc. doesn't trigger a reload per change.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DiscoverFilterDialog(
    initialFilters: DiscoverFilters,
    genres: List<Genre>,
    sortOptions: List<String>,
    onDismiss: () -> Unit,
    onApply: (DiscoverFilters) -> Unit,
    keywordRepository: KeywordRepository = KeywordRepositoryImpl(),
) {
    var year by remember { mutableStateOf(initialFilters.year) }
    var sortBy by remember { mutableStateOf(initialFilters.sortBy) }
    var selectedGenreIds by remember { mutableStateOf(initialFilters.genreIds) }
    var selectedKeywords by remember { mutableStateOf(initialFilters.keywords) }

    var yearMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var keywordQuery by remember { mutableStateOf("") }
    var keywordSuggestions by remember { mutableStateOf<List<Keyword>>(emptyList()) }
    var keywordMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(keywordQuery) {
        if (keywordQuery.isBlank()) {
            keywordSuggestions = emptyList()
        } else {
            delay(DiscoverFilterDialogConstant.KEYWORD_DEBOUNCE_MILLIS)
            keywordSuggestions = keywordRepository.searchKeywords(keywordQuery)
        }
        keywordMenuExpanded = keywordSuggestions.isNotEmpty()
    }

    val currentYear = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()).year }
    val years = remember { (currentYear downTo (currentYear - DiscoverFilterDialogConstant.YEAR_RANGE_SIZE)).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        // The default AlertDialog surface reads as flat/generic - surfaceContainerHigh is this
        // app's own tonal-elevation token (derived by lightColorScheme()/darkColorScheme() in
        // theme/Theme.kt from the vivid palette in theme/Color.kt), just not used anywhere yet.
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(stringResource(Res.string.discover_title)) },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(max = DiscoverFilterDialogConstant.DIALOG_MAX_HEIGHT)
                        .verticalScroll(rememberScrollState()),
            ) {
                FilterSectionHeader(
                    label = stringResource(Res.string.discover_filter_keywords_label),
                    showClear = selectedKeywords.isNotEmpty(),
                    onClear = { selectedKeywords = emptyList() },
                )
                // The applied keywords themselves - not just their ids - travel with the filter set
                // for exactly this: reopening the dialog needs to show *which* keywords are active,
                // with a chip to remove each one, not only a count.
                if (selectedKeywords.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                        selectedKeywords.forEach { keyword ->
                            FilterChip(
                                selected = true,
                                onClick = { selectedKeywords = selectedKeywords - keyword },
                                label = { Text(keyword.name) },
                                leadingIcon = { Icon(imageVector = Icons.Filled.Done, contentDescription = null) },
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = keywordQuery,
                    onValueChange = { keywordQuery = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.discover_filter_keywords_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // Deliberately an inline block in the normal layout flow, not a Popup-based
                // ExposedDropdownMenu: that positioning is computed against the current visible
                // window, and with the on-screen keyboard open (this field sits right at the top of
                // the dialog) it had room to flip the menu *above* the field instead of below,
                // hiding what the user was typing. Laying it out directly under the field guarantees
                // "below" regardless of keyboard/window state - the tonal Surface still reads as a
                // distinct dropdown-like block rather than a plain list.
                if (keywordMenuExpanded && keywordSuggestions.isNotEmpty()) {
                    Surface(
                        tonalElevation = 3.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .heightIn(
                                        max = DiscoverFilterDialogConstant.SUGGESTIONS_MAX_HEIGHT,
                                    ).verticalScroll(rememberScrollState()),
                        ) {
                            keywordSuggestions.forEach { keyword ->
                                Text(
                                    text = keyword.name,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (keyword !in selectedKeywords) selectedKeywords = selectedKeywords + keyword
                                                keywordQuery = ""
                                                keywordSuggestions = emptyList()
                                                keywordMenuExpanded = false
                                            }.padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = yearMenuExpanded,
                    onExpandedChange = { yearMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    OutlinedTextField(
                        value = year?.toString() ?: stringResource(Res.string.discover_filter_year_any),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.discover_filter_year_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    DropdownMenu(expanded = yearMenuExpanded, onDismissRequest = { yearMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.discover_filter_year_any)) },
                            onClick = {
                                year = null
                                yearMenuExpanded = false
                            },
                        )
                        years.forEach { candidateYear ->
                            DropdownMenuItem(
                                text = { Text(candidateYear.toString()) },
                                onClick = {
                                    year = candidateYear
                                    yearMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = sortMenuExpanded,
                    onExpandedChange = { sortMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val sortLabel = sortByLabelResource(sortBy)?.let { stringResource(it) } ?: sortBy
                    OutlinedTextField(
                        value = sortLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.discover_filter_sort_by_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(sortByLabelResource(option)?.let { stringResource(it) } ?: option) },
                                onClick = {
                                    sortBy = option
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                FilterSectionHeader(
                    label = stringResource(Res.string.discover_filter_genres_label),
                    showClear = selectedGenreIds.isNotEmpty(),
                    onClear = { selectedGenreIds = emptySet() },
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    genres.forEach { genre ->
                        val selected = selectedGenreIds.contains(genre.id)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedGenreIds =
                                    if (selected) selectedGenreIds - genre.id else selectedGenreIds + genre.id
                            },
                            label = { Text(genre.name) },
                            leadingIcon =
                                if (selected) {
                                    { Icon(imageVector = Icons.Filled.Done, contentDescription = null) }
                                } else {
                                    null
                                },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            // Filled, not a TextButton like "Close" below - Apply is the dominant action here, and
            // reading as equal-weight to dismiss was part of the dialog looking flat/generic.
            Button(
                onClick = {
                    onApply(DiscoverFilters(genreIds = selectedGenreIds, keywords = selectedKeywords, year = year, sortBy = sortBy))
                },
            ) {
                Text(stringResource(Res.string.discover_filter_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}

/** A section label with a "Clear" action, shown only once there's something in that section to clear. */
@Composable
private fun FilterSectionHeader(
    label: String,
    showClear: Boolean,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (showClear) {
            TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(stringResource(Res.string.discover_filter_clear), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
