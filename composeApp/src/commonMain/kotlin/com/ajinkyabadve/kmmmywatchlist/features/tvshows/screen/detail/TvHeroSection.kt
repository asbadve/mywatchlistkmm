package com.ajinkyabadve.kmmmywatchlist.features.tvshows.screen.detail

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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.heroColors
import com.ajinkyabadve.kmmmywatchlist.core.ui.hero.heroScrimBrush
import com.ajinkyabadve.kmmmywatchlist.core.usecase.FindYoutubeTrailerUseCase
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.HeroWatchOption
import com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail.heroWatchOption
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.TvDetail
import com.ajinkyabadve.kmmmywatchlist.openUrl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.hero_play_trailer
import mywatchlist.composeapp.generated.resources.hero_rating
import mywatchlist.composeapp.generated.resources.hero_watch_on
import mywatchlist.composeapp.generated.resources.tv_hero_episodes
import mywatchlist.composeapp.generated.resources.tv_hero_next_episode
import mywatchlist.composeapp.generated.resources.tv_hero_season
import mywatchlist.composeapp.generated.resources.tv_hero_seasons
import org.jetbrains.compose.resources.stringResource

private object TvHeroConstant {
    const val RELEASE_YEAR_LENGTH = 4
    const val ONGOING_SURFACE_ALPHA = 0.12f
    const val ONGOING_BORDER_ALPHA = 0.35f
    const val NEXT_EPISODE_TEXT_ALPHA = 0.62f
    const val CONTENT_RATING_TEXT_ALPHA = 0.85f
    const val CONTENT_RATING_BORDER_ALPHA = 0.35f
    const val OVERLAY_TEXT_ALPHA = 0.75f
    const val NETWORK_TEXT_ALPHA = 0.8f
}

/**
 * The movie hero, extended with what only a series needs.
 *
 * A film's hero answers "what is this and where can I watch it". A series has two more questions
 * behind it - is it still running, and how much of it is there - so status, season and episode
 * counts sit in the same glanceable block rather than in separate chip rows further down.
 */
@Composable
fun TvHeroSection(
    detail: TvDetail,
    // Injectable for the same reason as the movie hero: `openUrl` is a platform expect, so a test
    // clicking the button would launch a real browser rather than record the tap.
    onOpenUrl: (String) -> Unit = { openUrl(it) },
) {
    val density = LocalDensity.current.density
    val colors = heroColors()
    val backdropUrl =
        ImageConfigResolver.resolve(
            path = detail.backdropPath,
            type = ImageConfigResolver.ImageType.BACKDROP,
            targetWidthDp = HeroConstant.BACKDROP_TARGET_WIDTH_DP,
            density = density,
        )
    val watchOption = detail.watchProviders.heroWatchOption(Locale.current.region.uppercase())

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
        Box(modifier = Modifier.fillMaxSize().background(heroScrimBrush()))

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            if (detail.isOngoing) {
                OngoingBadge(status = detail.status.orEmpty(), colors = colors)
            }
            Text(
                text = detail.title,
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onHero,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (detail.isOngoing) 8.dp else 0.dp),
            )
            TvMetaRow(detail = detail, colors = colors, modifier = Modifier.padding(top = 8.dp))
            detail.heroEpisode()?.let { episode ->
                Text(
                    text = stringResource(Res.string.tv_hero_next_episode, episode.seasonNumber, episode.episodeNumber, episode.name),
                    fontSize = 12.sp,
                    color = colors.onHero.copy(alpha = TvHeroConstant.NEXT_EPISODE_TEXT_ALPHA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            watchOption?.let { option ->
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    option.allProviders.take(HeroConstant.MAX_PROVIDER_CHIPS).forEach { HeroProviderChip(provider = it) }
                }
            }
            TvActionRow(
                detail = detail,
                watchOption = watchOption,
                colors = colors,
                onOpenUrl = onOpenUrl,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

/**
 * A dot plus a word, not just a word. Whether a show is still running is the kind of thing a viewer
 * scans for rather than reads, and a coloured dot survives being glanced at.
 */
@Composable
private fun OngoingBadge(
    status: String,
    colors: HeroColors,
) {
    val green = colors.ongoing
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(green.copy(alpha = TvHeroConstant.ONGOING_SURFACE_ALPHA))
                .border(1.dp, green.copy(alpha = TvHeroConstant.ONGOING_BORDER_ALPHA), RoundedCornerShape(20.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(green))
        Text(
            // The raw TMDB status ("Returning Series", "In Production") is the useful wording here.
            text = status,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = green,
        )
    }
}

@Composable
private fun TvMetaRow(
    detail: TvDetail,
    colors: HeroColors,
    modifier: Modifier = Modifier,
) {
    val facts =
        buildList {
            detail.firstAirDate
                .take(TvHeroConstant.RELEASE_YEAR_LENGTH)
                .takeIf { it.isNotEmpty() }
                ?.let { add(it) }
            detail.numberOfSeasons?.takeIf { it > 0 }?.let {
                add(if (it == 1) stringResource(Res.string.tv_hero_season) else stringResource(Res.string.tv_hero_seasons, it))
            }
            detail.numberOfEpisodes?.takeIf { it > 0 }?.let { add(stringResource(Res.string.tv_hero_episodes, it)) }
            detail.voteAverage.takeIf { it > 0.0 }?.let { add(stringResource(Res.string.hero_rating, it.toOneDecimalString())) }
        }
    val rating = detail.usContentRating()
    val network = detail.networks?.firstOrNull()?.name
    if (facts.isEmpty() && rating == null && network == null) return

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            rating?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onHero.copy(alpha = TvHeroConstant.CONTENT_RATING_TEXT_ALPHA),
                    modifier =
                        Modifier
                            .border(
                                1.dp,
                                colors.onHero.copy(alpha = TvHeroConstant.CONTENT_RATING_BORDER_ALPHA),
                                RoundedCornerShape(4.dp),
                            ).padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
            if (facts.isNotEmpty()) {
                Text(
                    text = facts.joinToString(HeroConstant.META_SEPARATOR),
                    fontSize = 13.sp,
                    color = colors.onHero.copy(alpha = TvHeroConstant.OVERLAY_TEXT_ALPHA),
                    modifier = Modifier.padding(start = if (rating == null) 0.dp else 10.dp),
                )
            }
        }
        network?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onHero.copy(alpha = TvHeroConstant.NETWORK_TEXT_ALPHA),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TvActionRow(
    detail: TvDetail,
    watchOption: HeroWatchOption?,
    colors: HeroColors,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trailerUrl = remember(detail.videos) { FindYoutubeTrailerUseCase()(detail.videos) }
    if (watchOption == null && trailerUrl == null) return

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        watchOption?.let { option ->
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onOpenUrl(option.link) }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(Res.string.hero_watch_on, option.provider.providerName),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailerUrl?.let { url ->
            if (watchOption == null) {
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.buttonSurface)
                            .border(1.dp, colors.buttonOutline, RoundedCornerShape(8.dp))
                            .clickable { onOpenUrl(url) }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = colors.onHero,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(Res.string.hero_play_trailer),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onHero,
                    )
                }
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
