package com.ajinkyabadve.kmmmywatchlist.core

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED,
    ;

    // Factory method that creates an instance of the class based on window width
    companion object {

        fun basedOnWindowSizeClass(windowWidthType: String): WindowSize {
            return when (windowWidthType) {
                "WindowWidthSizeClass.Compact" -> COMPACT
                "WindowWidthSizeClass.Medium" -> MEDIUM
                else -> EXPANDED
            }
        }

        fun basedOnWidth(windowWidth: Dp): WindowSize {
            return when {
                windowWidth < 600.dp -> COMPACT
                windowWidth < 840.dp -> MEDIUM
                else -> EXPANDED
            }
        }
    }
}

fun WindowSizeClass.getGridColumn(): Int {
    return when (WindowSize.basedOnWindowSizeClass(widthSizeClass.toString())) {
        WindowSize.COMPACT -> {
            2
        }

        WindowSize.EXPANDED -> {
            6
        }

        WindowSize.MEDIUM -> {
            3
        }
    }
}