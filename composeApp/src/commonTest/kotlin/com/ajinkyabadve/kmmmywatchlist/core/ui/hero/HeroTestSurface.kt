package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private object HeroTestSurfaceConstant {
    /** Any width whose 4:5 height clears the 768px test root; 400 leaves room to spare. */
    const val WIDTH_DP = 400
}

/**
 * Constrains a hero to a width its buttons can be clicked at.
 *
 * A hero is [HeroConstant.HERO_ASPECT_RATIO] of whatever width it is given, so at the test root's
 * full 1024x768 it stands 1280 tall and its action row sits below the viewport - composed and
 * findable, but rejected by the input dispatcher as outside the root bounds. Both hero tests hit
 * this, so the workaround lives once beside them. Assertion-only tests do not need it.
 */
@Composable
fun HeroTestSurface(content: @Composable () -> Unit) {
    Box(modifier = Modifier.width(HeroTestSurfaceConstant.WIDTH_DP.dp)) { content() }
}
