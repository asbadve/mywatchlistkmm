package com.ajinkyabadve.kmmmywatchlist.previews

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ajinkyabadve.kmmmywatchlist.design.searchbox.SearchBox

@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    name = "Light Theme",
)
@Composable
fun SearchBoxPreview() {
    SearchBox(onClick = {})
}

@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Light Theme",
)
@Composable
fun SearchBoxPreviewNight() {
    SearchBox(onClick = {})
}
