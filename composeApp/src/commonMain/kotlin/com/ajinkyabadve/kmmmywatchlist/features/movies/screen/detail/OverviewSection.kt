package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.section_overview
import org.jetbrains.compose.resources.stringResource

@Composable
fun OverviewSection(overview: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.section_overview),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = overview.ifEmpty { "No overview available." },
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
    }
}
