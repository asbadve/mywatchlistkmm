package com.ajinkyabadve.kmmmywatchlist.features.discover.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.core.screen.Screen

class TrendingScreen : Screen {
    @Composable
    override fun Content() {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.fillMaxWidth().fillMaxHeight(),
        ) {
            Box {
                Text(text = "Trending", textAlign = TextAlign.Center)
            }
        }
    }
}
