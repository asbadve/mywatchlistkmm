package com.ajinkyabadve.kmmmywatchlist.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.back_content_description
import org.jetbrains.compose.resources.stringResource

private const val HERO_FADE_MILLIS = 300
private const val HERO_SCRIM_ALPHA = 0.4f

/**
 * The one top bar every detail screen uses, so that back affordance, colours and divider cannot
 * drift apart between them - they had already drifted to `Close` on some screens and `ArrowBack` on
 * others for the same gesture.
 *
 * Two looks, chosen by [isScrolledPastHero]:
 * - `null` - an ordinary in-flow bar, solid from the start. Goes in `Scaffold(topBar = )` with a
 *   [scrollBehavior] so Material3 collapses it.
 * - non-null - an overlay drawn over a backdrop image, transparent until the caller reports the
 *   list has scrolled past that image. These callers place the bar inside their own content (its
 *   measured height feeds the split layout) and collapse it via [collapsingTopBar] on [modifier],
 *   so they pass no [scrollBehavior].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    title: String,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isScrolledPastHero: Boolean? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showBackButton: Boolean = true,
) {
    val isOverHero = isScrolledPastHero == false
    val isSolid = !isOverHero

    val containerColor by animateColorAsState(
        targetValue = if (isSolid) MaterialTheme.colorScheme.background else Color.Transparent,
        animationSpec = tween(durationMillis = HERO_FADE_MILLIS),
    )

    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                // Over a backdrop the image carries the context, and the title would fight it;
                // it fades in as the bar goes solid. In-flow bars always show it.
                AnimatedVisibility(visible = isSolid, enter = fadeIn(), exit = fadeOut()) {
                    Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(
                        onClick = onBackClicked,
                        // Over the image there is no bar behind the icon, so it gets its own scrim
                        // to stay legible against whatever the backdrop happens to be.
                        modifier =
                            if (isOverHero) {
                                Modifier.background(Color.Black.copy(alpha = HERO_SCRIM_ALPHA), CircleShape)
                            } else {
                                Modifier
                            },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back_content_description),
                            tint = if (isOverHero) Color.White else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            // scrolledContainerColor is pinned to the same colour on purpose: with a scrollBehavior
            // attached, Material3 interpolates container -> scrolledContainer as the bar collapses,
            // and the default scrolledContainerColor is a lighter elevated tone - leaving it out
            // makes the bar change colour on the way down.
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                ),
            scrollBehavior = scrollBehavior,
        )
        // Nothing to divide from while the bar floats over an image.
        if (isSolid) {
            HorizontalDivider()
        }
    }
}
