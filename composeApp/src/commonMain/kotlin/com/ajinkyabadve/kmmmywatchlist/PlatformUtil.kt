package com.ajinkyabadve.kmmmywatchlist

import androidx.compose.runtime.Composable

expect fun getPlatformName(): String

expect fun createSettings(): com.russhwolf.settings.Settings