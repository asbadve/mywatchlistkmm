package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.CrewMember
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.label_directed_by_prefix
import org.jetbrains.compose.resources.stringResource

/**
 * "Directed by" line shared between the movie and TV episode detail screens - each director's
 * name is individually tappable to their person screen, unlike [directors]' plain-text neighbors
 * (writers, other crew) which have no per-person destination worth linking here.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectedByLine(
    directors: List<CrewMember>,
    onPersonClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (directors.isEmpty()) return
    FlowRow(modifier = modifier) {
        Text(
            text = stringResource(Res.string.label_directed_by_prefix),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
        directors.forEachIndexed { index, director ->
            Text(
                text = if (index < directors.lastIndex) "${director.name}, " else director.name,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onPersonClicked(director.id) },
            )
        }
    }
}
