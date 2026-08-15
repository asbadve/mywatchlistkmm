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
    const val TOP_SCRIM_END = 0.28f
    const val MID_SCRIM_START = 0.55f
    const val BASE_FADE_START = 0.82f
    const val PROVIDER_LOGO_TARGET_WIDTH_DP = 48
    const val CHIP_TEXT_ALPHA = 0.9f

    // The on-photo scrim's stops, flattened from the design's two separate overlays (a top wash
    // over the upper 30%, and a bottom ramp over the lower 78%) into one gradient.
    const val PHOTO_TOP_SCRIM_END = 0.30f
    const val PHOTO_MID_SCRIM_START = 0.52f
    const val PHOTO_BASE_FADE_START = 0.81f

    /** Where the on-photo scrim lands at the very bottom, instead of the page background. */
    const val PHOTO_SCRIM_TAIL = 0xFF08070A
}

/**
 * The gradient laid over a hero backdrop, doing three separate jobs in one pass: push the top away
 * from the backdrop so the back button survives a bright frame, do the same across the lower middle
 * so the title and facts have a base, and fade to the page background so the artwork dissolves into
 * the content rather than ending on a hard edge.
 *
 * Which direction "away from the backdrop" means is the whole point: dark theme darkens toward
 * black, light theme lightens toward the page surface, and the foregrounds drawn on top flip with
 * it. Half of this used to be hardcoded black under unconditionally white content, which made every
 * hero's lower half invisible in light theme.
 */
@Composable
fun heroScrimBrush(): Brush {
    val colors = heroColors()
    return Brush.verticalGradient(
        colorStops =
            arrayOf(
                0f to colors.scrim.copy(alpha = colors.topScrimAlpha),
                HeroComponentConstant.TOP_SCRIM_END to Color.Transparent,
                HeroComponentConstant.MID_SCRIM_START to colors.scrim.copy(alpha = colors.midScrimAlpha),
                HeroComponentConstant.BASE_FADE_START to
                    MaterialTheme.colorScheme.background.copy(alpha = colors.baseFadeAlpha),
                1f to MaterialTheme.colorScheme.background,
            ),
    )
}

/**
 * The scrim for a hero treated as a photographic panel: fixed in both themes, and ending in its own
 * darkness rather than fading to the page background.
 *
 * The trade-off against [heroScrimBrush] is the bottom stop. That one dissolves the artwork into the
 * page, so the hero has no edge; this one keeps the panel intact, which in light theme means the
 * hero meets the content below it as a visible boundary. That is the design's intent, not a defect -
 * but it is the first thing to look at if the screen reads as two disconnected halves.
 */
@Composable
internal fun heroOnPhotoScrimBrush(): Brush {
    val colors = HeroColors.onPhoto()
    return Brush.verticalGradient(
        colorStops =
            arrayOf(
                0f to colors.scrim.copy(alpha = colors.topScrimAlpha),
                HeroComponentConstant.PHOTO_TOP_SCRIM_END to Color.Transparent,
                HeroComponentConstant.PHOTO_MID_SCRIM_START to colors.scrim.copy(alpha = colors.midScrimAlpha),
                HeroComponentConstant.PHOTO_BASE_FADE_START to colors.scrim.copy(alpha = colors.baseFadeAlpha),
                1f to Color(HeroComponentConstant.PHOTO_SCRIM_TAIL),
            ),
    )
}

/** Names a streaming service the title is available on, beside the hero's primary button. */
@Composable
internal fun HeroProviderChip(
    provider: WatchProvider,
    colors: HeroColors,
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
                .background(colors.chipSurface)
                .border(1.dp, colors.chipOutline, RoundedCornerShape(50))
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
            color = colors.onHero.copy(alpha = HeroComponentConstant.CHIP_TEXT_ALPHA),
            maxLines = 1,
        )
    }
}
