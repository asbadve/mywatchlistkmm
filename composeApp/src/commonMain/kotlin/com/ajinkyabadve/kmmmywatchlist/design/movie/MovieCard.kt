package com.ajinkyabadve.kmmmywatchlist.design.movie

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.design.util.addShimmerLoadingAnimation
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_onSurface
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_light_onSurface

@Composable
fun MediaCard(
    modifier: Modifier = Modifier,
    movieTitle: String,
    painter: Painter?,
    accessibilityContentDescription: String? = null,
    onClick: () -> Unit,
    isLoadingState: Boolean = false,
) {
    if (isLoadingState) {
        Box(
            modifier =
                modifier
                    .aspectRatio(2 / 3f)
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true, properties = {
                        contentDescription =
                            accessibilityContentDescription ?: "$movieTitle, double tap to activate"
                    }).addShimmerLoadingAnimation(),
        )
    } else {
        painter?.let {
            InternalMediaCard(
                modifier = modifier,
                accessibilityContentDescription = accessibilityContentDescription,
                movieTitle = movieTitle,
                onClick = onClick,
                painter = painter,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun InternalMediaCard(
    modifier: Modifier,
    accessibilityContentDescription: String?,
    movieTitle: String,
    onClick: () -> Unit,
    painter: Painter,
) {
    Card(
        modifier =
            modifier
                .aspectRatio(2 / 3f)
                .fillMaxWidth()
                .semantics(mergeDescendants = true, properties = {
                    contentDescription =
                        accessibilityContentDescription ?: "$movieTitle, double tap to activate"
                }),
        border = BorderStroke(1.0.dp, "#44483E".toColor()), //
        shape = RoundedCornerShape(4.dp),
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                modifier = modifier.fillMaxSize(),
                painter = painter,
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )

            Text(
                text = movieTitle,
                color =if (isSystemInDarkTheme()) md_theme_light_onSurface else md_theme_dark_onSurface,
                maxLines = 1,
                minLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {}
                        .background(
                            Brush.verticalGradient(
                                0F to Color.Transparent,
                                .5F to Color.Black.copy(alpha = 0.5F),
                                1F to Color.Black.copy(alpha = 0.8F),
                            ),
                        )
                        .padding(8.dp)
                        .align(Alignment.BottomStart),
            )
        }
    }
}

// TODO move to appropriate class or find another solution
private fun String.toColor(): Color = Color(this.removePrefix("#").toLong(16) or 0x00000000FF000000)

// @Preview(name = "Light Mode")
// @Preview(
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//    showBackground = true,
//    name = "Dark Mode",
// )
// @Preview(
//    name = "Full Preview",
//    showSystemUi = true,
// )
// @Composable
// fun MovieCardPreview() {
//    MywatchlistTheme {
//        Box(
//            modifier = Modifier.padding(16.dp),
//            contentAlignment = Alignment.Center,
//        ) {
//            MovieCard(
//                movieTitle = "The Beauty and the Beast",
//                painter = painterResource(id = R.drawable.poster),
//            )
//        }
//    }
// }
