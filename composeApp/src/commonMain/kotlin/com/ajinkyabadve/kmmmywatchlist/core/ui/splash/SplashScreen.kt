package com.ajinkyabadve.kmmmywatchlist.core.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ajinkyabadve.kmmmywatchlist.isReducedMotionEnabled
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_background
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_onBackground
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_onPrimaryContainer
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_surface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource

private object SplashScreenConstant {
    // Matches the "3b" (Reel spin-up) animation spec captured in shipped_features.md
    // item 12, pulled from the MyWatchList Logo design file's own CSS keyframes.
    const val ICON_DURATION_MILLIS = 850
    const val MYWATCH_DELAY_MILLIS = 500L
    const val MYWATCH_DURATION_MILLIS = 700
    const val LIST_DELAY_MILLIS = 850L
    const val LIST_DURATION_MILLIS = 700
    const val TOTAL_DURATION_MILLIS = 1800L
    const val REDUCED_MOTION_HOLD_MILLIS = 400L

    // cubic-bezier(.2,.9,.2,1) from the design file - the icon reel motion and the "List" drop
    // both use this exact curve.
    val ReelEasing = CubicBezierEasing(0.2f, 0.9f, 0.2f, 1f)

    // CSS's plain "ease-out" - the "MyWatch" fade uses this one instead.
    val StandardEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

    val ICON_SIZE = 96.dp

    // Keyframe value stops as (progress, value) pairs, straight from the design file's percentage
    // keyframes - interpolated linearly between the surrounding pair once `progress` itself has
    // already been eased (matches how a browser resolves a CSS keyframe animation: one easing
    // curve paces the 0-1 timeline, then keyframe values interpolate linearly along it).
    val ICON_ROTATION_DEGREES = listOf(0f to -14f, 0.6f to 4f, 1f to 0f)
    val ICON_SCALE = listOf(0f to 0.8f, 0.6f to 1.04f, 1f to 1f)
    val MYWATCH_LETTER_SPACING_EM = listOf(0f to 0.18f, 1f to -0.02f)
    val LIST_OFFSET_DP = listOf(0f to -38f, 0.7f to 4f, 1f to 0f)
    val LIST_ALPHA = listOf(0f to 0f, 0.7f to 1f, 1f to 1f)
}

private fun keyframeValue(
    progress: Float,
    stops: List<Pair<Float, Float>>,
): Float {
    for (i in 0 until stops.size - 1) {
        val (startFraction, startValue) = stops[i]
        val (endFraction, endValue) = stops[i + 1]
        if (progress <= endFraction) {
            val localProgress = if (endFraction == startFraction) 1f else (progress - startFraction) / (endFraction - startFraction)
            return startValue + (endValue - startValue) * localProgress.coerceIn(0f, 1f)
        }
    }
    return stops.last().second
}

/**
 * The app's launch splash - reproduces the "3b" (Reel spin-up) animation from the MyWatchList Logo
 * design file (see `shipped_features.md` item 12 for the exact spec this was built from),
 * as Compose animations rather than the design file's literal CSS. Shown once per process launch
 * from `App()`, ahead of `MainAppScreen` - on iOS/Desktop/JS only. Android skips this entirely
 * ([usesNativeAnimatedSplash] is true there): the same "3b" icon motion runs natively instead, via
 * `styles.xml`'s `Theme.MyWatchList.Splash` + `splash_icon_animated.xml` (an AnimatedVectorDrawable
 * converted from the design team's layered SVG export), so Android shows one splash, not two
 * back-to-back. That native path has no surface for the wordmark reveal (icon-only API), which is
 * why this Compose version - wordmark included - stays the splash for the other three platforms.
 *
 * Reuses the existing shipped `app_icon` drawable for the icon artwork rather than re-deriving the
 * design file's SVG path a second time. One deliberate simplification versus the design file: the
 * moving sprocket-hole strips (the perforations rolling inside the icon) are **not** reproduced -
 * they're baked as static, non-animated cutouts into `app_icon`'s own artwork already, and
 * animating a second overlay in exact registration with that baked-in artwork isn't something that
 * can be verified without an on-device visual check (this environment can't render/screenshot the
 * app - see the project's "user launches and verifies apps themselves" convention). The icon's
 * rotate/scale/opacity reveal and the two-part wordmark drop are the animation's primary identity
 * and are reproduced in full; the sprocket roll is a polish detail worth adding once this can be
 * iterated on visually together.
 *
 * Colors come from [md_theme_dark_background]/[md_theme_dark_surface]/etc. (the app's real dark
 * Material3 tokens) rather than the design file's literal hex constants, and the wordmark uses the
 * app's actual `FontFamily.Default` rather than the design file's Sora typeface, which this app
 * doesn't bundle anywhere else (`theme/Theme.kt` only ever sets `FontFamily.Default`) - both per
 * code-conventions §2e (honor what the platform/app already has before adding something new).
 *
 * Respects [isReducedMotionEnabled]: when true, every element renders straight at its settled
 * end-state and this just holds briefly before calling [onFinished], instead of animating through
 * the reveal.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    durationMillis: Long = SplashScreenConstant.TOTAL_DURATION_MILLIS,
) {
    val reducedMotion = remember { isReducedMotionEnabled() }
    val iconProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val mywatchProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    val listProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (reducedMotion) {
            delay(SplashScreenConstant.REDUCED_MOTION_HOLD_MILLIS)
        } else {
            launch {
                iconProgress.animateTo(
                    1f,
                    tween(SplashScreenConstant.ICON_DURATION_MILLIS, easing = SplashScreenConstant.ReelEasing),
                )
            }
            launch {
                delay(SplashScreenConstant.MYWATCH_DELAY_MILLIS)
                mywatchProgress.animateTo(
                    1f,
                    tween(SplashScreenConstant.MYWATCH_DURATION_MILLIS, easing = SplashScreenConstant.StandardEaseOut),
                )
            }
            launch {
                delay(SplashScreenConstant.LIST_DELAY_MILLIS)
                listProgress.animateTo(1f, tween(SplashScreenConstant.LIST_DURATION_MILLIS, easing = SplashScreenConstant.ReelEasing))
            }
            delay(durationMillis)
        }
        onFinished()
    }

    val iconRotation = keyframeValue(iconProgress.value, SplashScreenConstant.ICON_ROTATION_DEGREES)
    val iconScale = keyframeValue(iconProgress.value, SplashScreenConstant.ICON_SCALE)
    val mywatchLetterSpacing = keyframeValue(mywatchProgress.value, SplashScreenConstant.MYWATCH_LETTER_SPACING_EM)
    val listOffsetDp = keyframeValue(listProgress.value, SplashScreenConstant.LIST_OFFSET_DP)
    val listAlpha = keyframeValue(listProgress.value, SplashScreenConstant.LIST_ALPHA)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Brush.radialGradient(listOf(md_theme_dark_background, md_theme_dark_surface))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(SplashScreenConstant.ICON_SIZE)
                        .graphicsLayer {
                            rotationZ = iconRotation
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = iconProgress.value.coerceIn(0f, 1f)
                        },
            )
            Row {
                Text(
                    text = "MyWatch",
                    style =
                        TextStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            letterSpacing = mywatchLetterSpacing.em,
                            color = md_theme_dark_onBackground,
                        ),
                    modifier = Modifier.graphicsLayer { alpha = mywatchProgress.value.coerceIn(0f, 1f) },
                )
                Text(
                    text = "List",
                    style =
                        TextStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = md_theme_dark_onPrimaryContainer,
                        ),
                    modifier =
                        Modifier.graphicsLayer {
                            alpha = listAlpha
                            translationY = listOffsetDp.dp.toPx()
                        },
                )
            }
        }
    }
}
