package com.ajinkyabadve.kmmmywatchlist.features.trending.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.myfav_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
fun MyFavScreenTab(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.myfav_placeholder))
    }
}
