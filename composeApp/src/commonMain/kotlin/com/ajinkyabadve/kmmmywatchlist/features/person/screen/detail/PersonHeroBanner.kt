package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.HeroColors
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.heroColors
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCredit
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.person_hero_known_for
import org.jetbrains.compose.resources.stringResource

private object PersonHeroBannerConstant {
    const val BANNER_TARGET_WIDTH_DP = 780
    const val SCRIM_WASH_ALPHA = 0.45f
    const val FADE_TOP_ALPHA = 0.45f
    const val FADE_MID_ALPHA = 0.85f
    const val ATTRIBUTION_SURFACE_ALPHA = 0.55f
    const val ATTRIBUTION_BORDER_ALPHA = 0.16f
}

/**
 * Cinematic banner behind a person's header.
 *
 * TMDB gives people portraits but no backdrop, so this borrows the backdrop of the work they are
 * best known for (see `PersonDetail.heroBackdropCredit`). Because that image belongs to a title
 * rather than to the person, the banner says which title it is and is tappable - otherwise it reads
 * as a decorative stock image and quietly misattributes someone else's artwork.
 *
 * Drawn as a background layer: the caller lays the existing header over it, so the portrait and name
 * keep their position and simply gain something to sit against.
 */
@Composable
fun PersonHeroBanner(
    credit: PersonCredit,
    onCreditClicked: (PersonCredit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val colors = heroColors()
    val backdropUrl =
        ImageConfigResolver.resolve(
            path = credit.backdropPath,
            type = ImageConfigResolver.ImageType.BACKDROP,
            targetWidthDp = PersonHeroBannerConstant.BANNER_TARGET_WIDTH_DP,
            density = density,
        )

    Box(modifier = modifier) {
        val painter =
            rememberAsyncImagePainter(
                model = backdropUrl,
                filterQuality = FilterQuality.Medium,
            )
        // No success/failure branching: an unloaded painter simply draws nothing, and the scrim
        // fades to the page background, so a failed image degrades to the plain header rather than
        // to a grey slab. Keeping the attribution unconditional also keeps it honest - the credit
        // is named whenever the banner is shown at all.
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // The header's own text sits on top of this, in theme colours chosen for the page
        // background - so the banner has to be pushed well back or the name washes out over a
        // bright frame. A flat wash plus a fade into the page background does that, and leaves the
        // banner reading as texture behind the content rather than a photo competing with it.
        //
        // The wash takes the theme's scrim rather than a hardcoded black: pushing an image back
        // means moving it toward the surface behind it, and in light theme that is the page, not
        // darkness. Black here turned the backdrop muddy grey while the gradient above it lightened.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.scrim.copy(alpha = PersonHeroBannerConstant.SCRIM_WASH_ALPHA))
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = PersonHeroBannerConstant.FADE_TOP_ALPHA),
                                    MaterialTheme.colorScheme.background.copy(alpha = PersonHeroBannerConstant.FADE_MID_ALPHA),
                                    MaterialTheme.colorScheme.background,
                                ),
                        ),
                    ),
        )

        CreditAttribution(
            credit = credit,
            colors = colors,
            onClick = { onCreditClicked(credit) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** Names the title the backdrop came from, and opens it. */
@Composable
private fun CreditAttribution(
    credit: PersonCredit,
    colors: HeroColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(colors.scrim.copy(alpha = PersonHeroBannerConstant.ATTRIBUTION_SURFACE_ALPHA))
                // The pill sits where the gradient has already reached the page background, so in
                // light theme its fill is near-invisible against it. The outline is what keeps it
                // reading as a tappable pill rather than as loose text.
                .border(
                    1.dp,
                    colors.onHero.copy(alpha = PersonHeroBannerConstant.ATTRIBUTION_BORDER_ALPHA),
                    RoundedCornerShape(50),
                ).clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = colors.onHero,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(Res.string.person_hero_known_for, credit.displayTitle),
            color = colors.onHero,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
