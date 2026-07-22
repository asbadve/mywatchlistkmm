package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Review
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.action_close
import mywatchlist.composeapp.generated.resources.label_review_by
import mywatchlist.composeapp.generated.resources.section_reviews
import mywatchlist.composeapp.generated.resources.title_review_by
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReviewsSection(reviews: List<Review>) {
    if (reviews.isNotEmpty()) {
        var selectedReview by remember { mutableStateOf<Review?>(null) }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(Res.string.section_reviews),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                reviews.take(3).forEach { review ->
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedReview = review },
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.label_review_by, review.author),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = review.content,
                                fontSize = 13.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        selectedReview?.let { review ->
            AlertDialog(
                onDismissRequest = { selectedReview = null },
                title = { Text(text = stringResource(Res.string.title_review_by, review.author)) },
                text = {
                    Box(
                        modifier =
                            Modifier
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = review.content,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedReview = null }) {
                        Text(stringResource(Res.string.action_close))
                    }
                },
            )
        }
    }
}
