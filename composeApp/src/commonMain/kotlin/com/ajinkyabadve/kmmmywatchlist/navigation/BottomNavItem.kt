package com.ajinkyabadve.kmmmywatchlist.navigation

import org.jetbrains.compose.resources.DrawableResource

data class BottomNavItem(
    val key: AppKey,
    val label: String,
    val icon: DrawableResource,
)
