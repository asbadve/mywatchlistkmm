package com.ajinkyabadve.kmmmywatchlist.design.movie

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MovieCard(
    modifier: Modifier = Modifier,
    movieTitle: String,
    painter: Painter,
    accessibilityContentDescription: String? = null,
) {
    Card(
        modifier = modifier
            .width(210.dp)
            .fillMaxHeight()
            .semantics(mergeDescendants = true, properties = {
                contentDescription =
                    accessibilityContentDescription ?: "$movieTitle, double tap to activate"
            }),
        border = BorderStroke(1.0.dp, "#44483E".toColor()), //
        shape = RoundedCornerShape(4.dp),
    ) {
        Column {
            Image(
                modifier = modifier
                    .width(210.dp)
                    .height(275.dp),
                painter = painter,
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
            Text(
                text = movieTitle,
                modifier = modifier.fillMaxHeight().padding(8.dp).semantics(mergeDescendants = true) {},
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

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
