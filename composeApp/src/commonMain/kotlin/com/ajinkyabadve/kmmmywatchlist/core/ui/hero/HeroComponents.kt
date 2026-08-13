package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvider

private object HeroComponentConstant {
    const val TOP_SCRIM_ALPHA = 0.55f
    const val MID_SCRIM_ALPHA = 0.45f
    const val BASE_FADE_ALPHA = 0.92f
    const val TOP_SCRIM_END = 0.28f
    const val MID_SCRIM_START = 0.55f
    const val BASE_FADE_START = 0.82f
    const val PROVIDER_LOGO_TARGET_WIDTH_DP = 48
    const val CHIP_SURFACE_ALPHA = 0.10f
    const val CHIP_BORDER_ALPHA = 0.16f
    const val CHIP_TEXT_ALPHA = 0.9f
}

/**
 * The gradient laid over a hero backdrop, doing three separate jobs in one pass: darken the top so
 * the back button survives a bright frame, darken the lower middle so the title and facts have a
 * base, and fade to the page background so the artwork dissolves into the content rather than
 * ending on a hard edge.
 */
@Composable
fun heroScrimBrush(): Brush =
    Brush.verticalGradient(
        colorStops =
            arrayOf(
                0f to Color.Black.copy(alpha = HeroComponentConstant.TOP_SCRIM_ALPHA),
                HeroComponentConstant.TOP_SCRIM_END to Color.Transparent,
                HeroComponentConstant.MID_SCRIM_START to Color.Black.copy(alpha = HeroComponentConstant.MID_SCRIM_ALPHA),
                HeroComponentConstant.BASE_FADE_START to
                    MaterialTheme.colorScheme.background.copy(alpha = HeroComponentConstant.BASE_FADE_ALPHA),
                1f to MaterialTheme.colorScheme.background,
            ),
    )

/** Names a streaming service the title is available on, beside the hero's primary button. */
@Composable
fun HeroProviderChip(
    provider: WatchProvider,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val logoUrl =
        ImageConfigResolver.resolve(
            path = provider.logoPath,
            type = ImageConfigResolver.ImageType.LOGO,
            targetWidthDp = HeroComponentConstant.PROVIDER_LOGO_TARGET_WIDTH_DP,
            density = density,
        )
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = HeroComponentConstant.CHIP_SURFACE_ALPHA))
                .border(1.dp, Color.White.copy(alpha = HeroComponentConstant.CHIP_BORDER_ALPHA), RoundedCornerShape(50))
                .padding(start = 4.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = logoUrl, filterQuality = FilterQuality.Medium),
            contentDescription = null,
            modifier = Modifier.size(18.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = provider.providerName,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = HeroComponentConstant.CHIP_TEXT_ALPHA),
            maxLines = 1,
        )
    }
}
