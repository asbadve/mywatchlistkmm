package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.format.toOneDecimalString
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.HeroColors
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.HeroConstant
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.HeroProviderChip
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.heroOnPhotoScrimBrush
import com.ajinkyabadve.kmmmywatchlist.core.usecase.FindYoutubeTrailerUseCase
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.openUrl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.hero_play_trailer
import mywatchlist.composeapp.generated.resources.hero_rating
import mywatchlist.composeapp.generated.resources.hero_runtime_hours_minutes
import mywatchlist.composeapp.generated.resources.hero_runtime_minutes
import mywatchlist.composeapp.generated.resources.hero_watch_on
import org.jetbrains.compose.resources.stringResource

private object MovieHeroConstant {
    const val RELEASE_YEAR_LENGTH = 4
    const val OVERLAY_TEXT_ALPHA = 0.75f
    const val CERTIFICATION_BORDER_ALPHA = 0.40f
    const val META_ITEM_SPACING_DP = 9
    const val META_DOT_SIZE_DP = 3
    const val META_DOT_ALPHA = 0.5f
    const val TITLE_SHADOW_ALPHA = 0.6f
    const val TITLE_SHADOW_OFFSET_Y = 4f
    const val TITLE_SHADOW_BLUR = 24f
}

/**
 * Backdrop-led hero: the artwork runs full-bleed behind the title, facts and a single "watch on"
 * button, rather than sitting in a band with the text stacked underneath it.
 *
 * The provider button is the point. This app does not host anything, so the most useful thing the
 * hero can say is where a title actually streams - which makes the primary action a redirect, and
 * naming the service up front sets that expectation before the tap rather than after it.
 */
@Composable
fun MovieHeroSection(
    detail: MovieDetail,
    // Defaulted rather than passed by every caller: the platform `openUrl` is a top-level expect,
    // so a UI test clicking the button would launch a real browser instead of recording the tap.
    onOpenUrl: (String) -> Unit = { openUrl(it) },
) {
    val density = LocalDensity.current.density
    // Fixed in both themes: this hero is a photographic panel, and a photo does not get lighter
    // because the page around it did. Only the content below the hero follows the theme.
    val colors = HeroColors.onPhoto()
    val backdropUrl =
        ImageConfigResolver.resolve(
            path = detail.backdropPath,
            type = ImageConfigResolver.ImageType.BACKDROP,
            targetWidthDp = HeroConstant.BACKDROP_TARGET_WIDTH_DP,
            density = density,
        )
    val watchOption = detail.heroWatchOption(Locale.current.region.uppercase())

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(HeroConstant.HERO_ASPECT_RATIO)
                .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = backdropUrl, filterQuality = FilterQuality.Medium),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(modifier = Modifier.fillMaxSize().background(heroOnPhotoScrimBrush()))

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = detail.title,
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onHero,
                // The scrim handles an average frame; the shadow is what keeps the title readable
                // over the bright ones, where a fixed gradient alone cannot win.
                style =
                    TextStyle(
                        shadow =
                            Shadow(
                                color = Color.Black.copy(alpha = MovieHeroConstant.TITLE_SHADOW_ALPHA),
                                offset = Offset(0f, MovieHeroConstant.TITLE_SHADOW_OFFSET_Y),
                                blurRadius = MovieHeroConstant.TITLE_SHADOW_BLUR,
                            ),
                    ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            HeroMetaRow(detail = detail, colors = colors, modifier = Modifier.padding(top = 8.dp))
            watchOption?.let { option ->
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    option.allProviders
                        .take(HeroConstant.MAX_PROVIDER_CHIPS)
                        .forEach { HeroProviderChip(provider = it, colors = colors) }
                }
            }
            HeroActionRow(
                detail = detail,
                watchOption = watchOption,
                colors = colors,
                onOpenUrl = onOpenUrl,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

/** Year, runtime and rating as one quiet line, with the age rating called out as an outlined pill. */
@Composable
private fun HeroMetaRow(
    detail: MovieDetail,
    colors: HeroColors,
    modifier: Modifier = Modifier,
) {
    val facts =
        buildList {
            detail.releaseDate
                .take(MovieHeroConstant.RELEASE_YEAR_LENGTH)
                .takeIf { it.isNotEmpty() }
                ?.let { add(it) }
            detail.runtimeHoursAndMinutes()?.let { (hours, minutes) ->
                add(
                    if (hours > 0) {
                        stringResource(Res.string.hero_runtime_hours_minutes, hours, minutes)
                    } else {
                        stringResource(Res.string.hero_runtime_minutes, minutes)
                    },
                )
            }
            detail.voteAverage.takeIf { it > 0.0 }?.let {
                add(stringResource(Res.string.hero_rating, it.toOneDecimalString()))
            }
        }
    val certification = detail.usCertification()
    if (facts.isEmpty() && certification.isNullOrEmpty()) return

    // Year, age rating, runtime and score are peers, so the design separates them with a drawn dot
    // rather than an interpunct - it lets the age rating sit inline as a pill instead of being
    // bolted onto the front of a single joined string.
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MovieHeroConstant.META_ITEM_SPACING_DP.dp),
    ) {
        val leadingFact = facts.firstOrNull()
        val trailingFacts = facts.drop(1)

        leadingFact?.let { HeroFactText(text = it, colors = colors) }

        certification?.takeIf { it.isNotEmpty() }?.let {
            if (leadingFact != null) HeroMetaDot(colors = colors)
            Text(
                text = it,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onHero,
                modifier =
                    Modifier
                        .border(
                            1.dp,
                            colors.onHero.copy(alpha = MovieHeroConstant.CERTIFICATION_BORDER_ALPHA),
                            RoundedCornerShape(4.dp),
                        ).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }

        trailingFacts.forEach { fact ->
            HeroMetaDot(colors = colors)
            HeroFactText(text = fact, colors = colors)
        }
    }
}

/** One fact in the meta row, quieter than the title but still on the artwork. */
@Composable
private fun HeroFactText(
    text: String,
    colors: HeroColors,
) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = colors.onHero.copy(alpha = MovieHeroConstant.OVERLAY_TEXT_ALPHA),
    )
}

/** The drawn separator between meta items. */
@Composable
private fun HeroMetaDot(colors: HeroColors) {
    Box(
        modifier =
            Modifier
                .size(MovieHeroConstant.META_DOT_SIZE_DP.dp)
                .clip(CircleShape)
                .background(colors.onHero.copy(alpha = MovieHeroConstant.META_DOT_ALPHA)),
    )
}

/**
 * One filled primary action, plus the trailer as a secondary icon button.
 *
 * The trailer used to be a floating button in the middle of the artwork; sitting it next to the
 * provider button makes the ranking explicit - watching the thing beats watching an advert for it -
 * and stops two different play buttons competing on the same image. With nowhere to stream it, the
 * trailer is all that is left to offer, so it takes the labelled slot instead of leaving the row as
 * one unexplained icon.
 */
@Composable
private fun HeroActionRow(
    detail: MovieDetail,
    watchOption: HeroWatchOption?,
    colors: HeroColors,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trailerUrl = remember(detail.videos) { FindYoutubeTrailerUseCase()(detail.videos) }
    if (watchOption == null && trailerUrl == null) return

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        watchOption?.let { option ->
            HeroPrimaryButton(
                label = stringResource(Res.string.hero_watch_on, option.provider.providerName),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { onOpenUrl(option.link) },
            )
        }
        trailerUrl?.let { url ->
            if (watchOption == null) {
                HeroPrimaryButton(
                    label = stringResource(Res.string.hero_play_trailer),
                    containerColor = colors.buttonSurface,
                    contentColor = colors.onHero,
                    onClick = { onOpenUrl(url) },
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.buttonSurface)
                            .border(1.dp, colors.buttonOutline, RoundedCornerShape(8.dp))
                            .clickable { onOpenUrl(url) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(Res.string.hero_play_trailer),
                        tint = colors.onHero,
                    )
                }
            }
        }
    }
}

/** The hero's labelled action, shared by the provider button and the trailer-only fallback. */
@Composable
private fun HeroPrimaryButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(containerColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
