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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    /**
     * How many stops each half of the content wash is drawn with.
     *
     * A `Brush.verticalGradient` interpolates linearly between stops, so a two-stop ramp changes
     * slope abruptly at each end and that shows up as a line across the artwork. Sampling an eased
     * curve at several points is what makes it read as a wash rather than a band.
     */
    const val WASH_STEPS = 6
    const val PROVIDER_LOGO_TARGET_WIDTH_DP = 48
    const val CHIP_TEXT_ALPHA = 0.9f
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
internal fun heroScrimBrush(stops: HeroScrimStops = HeroScrimStops.unmeasured()): Brush {
    val colors = heroColors()
    val background = MaterialTheme.colorScheme.background
    val steps = HeroComponentConstant.WASH_STEPS

    val colorStops =
        buildList {
            add(0f to colors.scrim.copy(alpha = colors.topScrimAlpha))
            add(HeroComponentConstant.TOP_SCRIM_END to Color.Transparent)

            // Rising to the content wash. Eased rather than linear so there is no edge where the
            // artwork stops being untouched.
            for (step in 1..steps) {
                val progress = step / steps.toFloat()
                add(
                    lerp(stops.washStart, stops.fullStrengthAt, progress) to
                        colors.scrim.copy(alpha = smoothStep(progress) * colors.contentWashAlpha),
                )
            }

            // And on to the page background. This half used to hold flat at the content wash and
            // only fade at the very bottom, which put a change of slope right above the title -
            // read as a hard line across the hero. Carrying on upward removes it.
            for (step in 1..steps) {
                val progress = step / steps.toFloat()
                add(
                    lerp(stops.fullStrengthAt, 1f, progress) to
                        background.copy(
                            alpha = lerp(colors.contentWashAlpha, 1f, smoothStep(progress)),
                        ),
                )
            }
        }.toTypedArray()

    return Brush.verticalGradient(colorStops = colorStops)
}

/**
 * Tracks where a hero's bottom-aligned content column starts, as a fraction of hero height, so
 * [heroScrimBrush] can put the wash exactly there.
 *
 * Both heroes measure the same way, so the mechanism lives here rather than being written out twice
 * and drifting. Feed [onHeroSized] the hero box and [onContentSized] the content column.
 */
@Stable
internal class HeroContentMeasurement {
    private var heroHeightPx by mutableStateOf(0)
    private var contentHeightPx by mutableStateOf(0)

    val stops: HeroScrimStops
        get() =
            if (heroHeightPx <= 0 || contentHeightPx <= 0) {
                HeroScrimStops.unmeasured()
            } else {
                HeroScrimStops.forContentTop(1f - contentHeightPx.toFloat() / heroHeightPx)
            }

    fun onHeroSized(heightPx: Int) {
        heroHeightPx = heightPx
    }

    fun onContentSized(heightPx: Int) {
        contentHeightPx = heightPx
    }
}

@Composable
internal fun rememberHeroContentMeasurement(): HeroContentMeasurement = remember { HeroContentMeasurement() }

/** Names a streaming service the title is available on, beside the hero's primary button. */
@Composable
fun HeroProviderChip(
    provider: WatchProvider,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val colors = heroColors()
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
