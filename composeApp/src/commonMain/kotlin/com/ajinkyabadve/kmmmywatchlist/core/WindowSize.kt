package com.ajinkyabadve.kmmmywatchlist.core

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED,
    ;

    fun isCompact() = this == COMPACT

    fun isMedium() = this == MEDIUM

    fun isExpanded() = this == EXPANDED

    // Factory method that creates an instance of the class based on window width
    companion object {
        fun getWindowSize(windowSizeClass: WindowSizeClass) =
            basedOnWindowSizeClass(
                windowSizeClass.widthSizeClass,
            )

        fun basedOnWindowSizeClass(windowWidthType: WindowWidthSizeClass): WindowSize =
            when (windowWidthType) {
                WindowWidthSizeClass.Compact -> COMPACT
                WindowWidthSizeClass.Medium -> MEDIUM
                else -> EXPANDED
            }

        fun basedOnWidth(windowWidth: Dp): WindowSize =
            when {
                windowWidth < 600.dp -> COMPACT
                windowWidth < 840.dp -> MEDIUM
                else -> EXPANDED
            }
    }
}

fun WindowSizeClass.getGridColumn(): Int =
    when (WindowSize.basedOnWindowSizeClass(this.widthSizeClass)) {
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

fun WindowSizeClass.getHorizontalPadding(): Dp =
    when (WindowSize.basedOnWindowSizeClass(this.widthSizeClass)) {
        WindowSize.COMPACT -> {
            0.dp
        }

        WindowSize.EXPANDED -> {
            50.dp
        }

        WindowSize.MEDIUM -> {
            0.dp
        }
    }
