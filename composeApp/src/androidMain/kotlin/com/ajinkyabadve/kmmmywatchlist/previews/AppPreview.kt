package com.ajinkyabadve.kmmmywatchlist.previews

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ajinkyabadve.kmmmywatchlist.MainAppScreen
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize

@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    name = "Light Theme",
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun topAppBarPreview() {
    MainAppScreen(WindowSize.MEDIUM)
}

@Preview(
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Theme",
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun topAppBarDarkPreview() {
    MainAppScreen(WindowSize.MEDIUM)
}
