package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.core.format.toRegionFlagEmoji
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.notifications.RegionalRelease
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseSourceKind
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseType
import com.ajinkyabadve.kmmmywatchlist.features.notifications.regionalReleases
import com.ajinkyabadve.kmmmywatchlist.features.notifications.resolveReleaseDate
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.release_dates_dialog_title
import mywatchlist.composeapp.generated.resources.release_dates_first_worldwide
import mywatchlist.composeapp.generated.resources.release_type_digital
import mywatchlist.composeapp.generated.resources.release_type_physical
import mywatchlist.composeapp.generated.resources.release_type_premiere
import mywatchlist.composeapp.generated.resources.release_type_theatrical
import mywatchlist.composeapp.generated.resources.release_type_theatrical_limited
import mywatchlist.composeapp.generated.resources.release_type_tv
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private object RegionalReleaseDateChipConstant {
    val DIALOG_LIST_MAX_HEIGHT = 360.dp
    val ROW_SPACING = 10.dp
    const val SEPARATOR = " · "
}

/**
 * The movie page's release-date chip, showing the date that matters to this viewer: their
 * region's (then fallback region's) earliest theatrical, then digital release - the exact date a
 * release reminder fires on, via the same [resolveReleaseDate]. TMDB's primary `release_date` is
 * only the earliest release anywhere (often a festival or another country), which is why the chip
 * no longer shows it directly; the website itself shows a regional date with its country code.
 *
 * Tapping it lists every release TMDB has for that region (theatrical, digital, physical...) plus
 * the worldwide first-release date, like the TMDB site's "Release Dates" page.
 */
@Composable
internal fun RegionalReleaseDateChip(
    detail: MovieDetail,
    regionCode: String,
    fallbackRegionCode: String,
) {
    val resolved = remember(detail, regionCode, fallbackRegionCode) { detail.resolveReleaseDate(regionCode, fallbackRegionCode) } ?: return
    val region = resolved.source.regionCode
    val releases = remember(detail, region) { region?.let { detail.regionalReleases(it) }.orEmpty() }
    var showDialog by remember { mutableStateOf(false) }

    val label =
        buildString {
            if (region != null) append(region.toRegionFlagEmoji()).append(' ')
            append(formatFullReleaseDate(resolved.date.toString()))
            val typeLabel =
                when (resolved.source.kind) {
                    ReleaseSourceKind.THEATRICAL -> Res.string.release_type_theatrical
                    ReleaseSourceKind.DIGITAL -> Res.string.release_type_digital
                    else -> null
                }
            if (typeLabel != null) append(RegionalReleaseDateChipConstant.SEPARATOR).append(stringResource(typeLabel))
        }
    SuggestionChip(
        onClick = { if (releases.isNotEmpty()) showDialog = true },
        label = { Text(label) },
    )

    if (showDialog && region != null) {
        RegionalReleasesDialog(
            regionCode = region,
            releases = releases,
            worldwideFirstRelease = detail.releaseDate,
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun RegionalReleasesDialog(
    regionCode: String,
    releases: List<RegionalRelease>,
    worldwideFirstRelease: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.release_dates_dialog_title, "${regionCode.toRegionFlagEmoji()} $regionCode".trim())) },
        text = {
            Column(
                modifier =
                    Modifier
                        .heightIn(
                            max = RegionalReleaseDateChipConstant.DIALOG_LIST_MAX_HEIGHT,
                        ).verticalScroll(rememberScrollState()),
            ) {
                releases.forEach { release ->
                    Text(
                        text =
                            listOf(
                                stringResource(release.type.labelRes()),
                                formatFullReleaseDate(release.date.toString()),
                                release.certification,
                                release.note,
                            ).filter { it.isNotEmpty() }
                                .joinToString(RegionalReleaseDateChipConstant.SEPARATOR),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = RegionalReleaseDateChipConstant.ROW_SPACING),
                    )
                }
                if (worldwideFirstRelease.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.release_dates_first_worldwide, formatFullReleaseDate(worldwideFirstRelease)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) }
        },
    )
}

private fun ReleaseType.labelRes(): StringResource =
    when (this) {
        ReleaseType.PREMIERE -> Res.string.release_type_premiere
        ReleaseType.THEATRICAL_LIMITED -> Res.string.release_type_theatrical_limited
        ReleaseType.THEATRICAL -> Res.string.release_type_theatrical
        ReleaseType.DIGITAL -> Res.string.release_type_digital
        ReleaseType.PHYSICAL -> Res.string.release_type_physical
        ReleaseType.TV -> Res.string.release_type_tv
    }
