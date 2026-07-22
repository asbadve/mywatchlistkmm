package com.ajinkyabadve.kmmmywatchlist.design.util

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.addShimmerLoadingAnimation(targetValue: Float = 1000f): Modifier =
    composed {
        val shimmerColors = ShimmerAnimationData(isLightMode = !isSystemInDarkTheme()).getColours()

        val transition = rememberInfiniteTransition()
        val translateAnimation =
            transition.animateFloat(
                initialValue = 0f,
                targetValue = targetValue,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse,
                    ),
            )

        this.background(
            brush =
                Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset.Zero,
                    end = Offset(x = translateAnimation.value, y = translateAnimation.value),
                ),
            shape = RoundedCornerShape(4.dp),
        )
    }

data class ShimmerAnimationData(
    private val isLightMode: Boolean,
) {
    fun getColours(): List<Color> =
        if (isLightMode) {
            val color = Color.White

            listOf(
                color.copy(alpha = 0.3f),
                color.copy(alpha = 0.5f),
                color.copy(alpha = 1.0f),
                color.copy(alpha = 0.5f),
                color.copy(alpha = 0.3f),
            )
        } else {
            val color = Color.Black

            listOf(
                color.copy(alpha = 0.0f),
                color.copy(alpha = 0.3f),
                color.copy(alpha = 0.5f),
                color.copy(alpha = 0.3f),
                color.copy(alpha = 0.0f),
            )
        }
}
